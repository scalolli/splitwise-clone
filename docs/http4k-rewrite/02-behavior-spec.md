# Behavior Specification

This document defines what the Kotlin app must do. It is the user-visible contract for
the application. Where earlier implementations had known bugs or design gaps, the
correct behavior is documented here instead.

---

## Authentication

### Register
- `GET /register` — renders the registration form
- `POST /register`
  - Required fields: `username`, `email`, `password`, `confirm_password`
  - Validation:
    - All fields are required
    - `password` and `confirm_password` must match
    - `username` must be unique (case-sensitive)
    - `email` must be unique (case-insensitive)
  - On success: account created, redirect to `/login` with flash "Your account has been created"
  - On failure: re-render form with inline error messages

### Login
- `GET /login` — renders the login form
- `POST /login`
  - Required fields: `username`, `password`
  - On success: session cookie set, redirect to `/`
  - On failure: re-render form with "Invalid username or password"

### Logout
- `GET /logout` — clears session cookie, redirects to `/`, flash "You have been logged out"

### Session rules
- All routes except `GET /`, `GET /login`, `POST /login`, `GET /register`, `POST /register`
  require a valid session
- Unauthenticated access to any protected route: redirect to `/login` with flash
  "Please log in to access this page"

---

## Home page

- `GET /` — accessible without login
- Displays a list of all groups
- Displays a list of all registered users (usernames)

---

## Groups

### View group
- `GET /group/{id}` — protected
- Shows group name, description, member list
- Shows all expenses (description, amount, payer, date)
- Shows calculated balances (who owes whom, net amounts)
  - Balances subtract any recorded settlements for this group
- 404 if group does not exist

### Create group
- `GET /group/create` — protected, renders form
- `POST /group/create` — protected
  - Required: `name`
  - Optional: `description`
  - On success: group created with current user as creator and first member,
    redirect to `/group/{id}` with flash "Group created successfully"
  - On failure: re-render form with errors

### Edit group
- `GET /group/{id}/edit` — protected; only group creator may access
- `POST /group/{id}/edit` — protected; only group creator may submit
  - Updates `name` and `description`
  - Can add a member by username in the same form submission
  - On success: redirect to `/group/{id}` with flash "Group updated successfully"
  - On unauthorized: redirect to `/group/{id}` with flash
    "You do not have permission to edit this group"
  - On member not found: re-render with flash "User not found"
  - On member already in group: re-render with flash "User is already a member"

### Add member
- `POST /group/{id}/add_member` — protected; only group creator
  - Body: `username`
  - On success: redirect to `/group/{id}` with flash "Member added successfully"
  - On unauthorized: redirect to `/group/{id}` with flash
    "You do not have permission to add members to this group"
  - On user not found: flash "User not found"
  - On already member: flash "User is already a member of this group"

### Remove member
- `POST /group/{id}/remove_member/{userId}` — protected; only group creator
  - Cannot remove the group creator
  - On success: redirect to `/group/{id}` with flash "Member removed successfully"
  - On unauthorized: flash "You do not have permission to remove members from this group"
  - On attempt to remove creator: flash "Cannot remove the group creator"
  - On user not a member: flash "User is not a member of this group"

---

## Expenses

### Add expense
- `GET /group/{id}/add_expense` — protected; user must be a group member
- `POST /group/{id}/add_expense` — protected; user must be a group member
  - Required fields: `description`, `amount`, `payer_id`, at least one split entry
  - Validation (all of these must pass):
    1. `description` is non-empty
    2. `amount` is a positive decimal > 0
    3. `payer_id` is a current group member
    4. Each split `user_id` is a current group member
    5. No duplicate `user_id` values in splits
    6. The payer must appear in at least one split entry
    7. Sum of all split amounts must equal total `amount` (tolerance: ±0.01)
  - On success: expense saved, redirect to `/group/{id}` with flash "Expense added successfully"
  - On failure: re-render form with inline validation errors
  - Default split: all group members, amounts pre-filled as 0

### Edit expense
- `GET /expenses/{id}/edit` — protected
  - User must be the expense payer OR the group creator; others get 403
- `POST /expenses/{id}/edit` — protected; same authorization as GET
  - Same validation rules as add expense
  - On success: splits replaced atomically, redirect to `/group/{id}` with flash
    "Expense updated successfully"
  - On failure: re-render form with inline errors
  - 404 if expense does not exist

### Delete expense
- `POST /expenses/{id}/delete` — protected
  - Implemented from the start
  - User must be the expense payer OR the group creator
  - All associated expense shares are deleted (cascade)
  - On success: redirect to `/group/{id}` with flash "Expense deleted"
  - On unauthorized: flash "You do not have permission to delete this expense"
  - 404 if expense does not exist

---

## Balances

### Calculation rules
- For each expense: non-payer members owe the payer their share amount
- Net pairwise debts are simplified: if A owes B £30 and B owes A £10, result is A owes B £20
- Settled amounts reduce the displayed balance
  - If A owes B £20 (net from expenses) and A has settled £15 to B, display is A owes B £5
  - If settled amount equals or exceeds debt, no balance is shown for that pair
- Balances always include settlements

---

## Settlements

### Record settlement
- `POST /group/{id}/settle` — protected; user must be a group member
  - Body: `from_user_id`, `to_user_id`, `amount`
  - Validation:
    - Both users must be group members
    - Amount must be > 0
    - `from_user_id` and `to_user_id` must be different users
  - On success: settlement recorded, redirect to `/group/{id}` with flash
    "Settlement recorded"
  - On failure: re-render with errors

### Settlement history
- `GET /group/{id}/settlements` — protected; user must be a group member
  - Lists all settlements for the group in descending date order
  - Shows: who paid, who received, amount, date

---

## Authorization matrix

| Action | Who can perform it |
|---|---|
| View any page | Any authenticated user (except public pages) |
| View group detail | Any authenticated user (not restricted to members) |
| Add expense | Group members only |
| Edit expense | Expense payer OR group creator |
| Delete expense | Expense payer OR group creator |
| Edit group | Group creator only |
| Add member to group | Group creator only |
| Remove member from group | Group creator only (cannot remove self) |
| Record settlement | Group members only |
| View settlement history | Group members only |

---

## Validation error messages

These messages must match exactly, as they form part of the user-visible contract:

| Context | Condition | Message |
|---|---|---|
| Register | Username taken | "Username already exists" |
| Register | Email taken | "Email already exists" |
| Register | Passwords don't match | "Passwords do not match" |
| Login | Invalid credentials | "Invalid username or password" |
| Group create | Missing name | "Group name is required" |
| Add/edit expense | Payer not in group | "Payer is not a member of this group" |
| Add/edit expense | Split sum ≠ total | "The sum of all splits must equal the total amount. Current total: £{x}" |
| Add/edit expense | Payer not in splits | "The payer must be included in the expense splits" |
| Add/edit expense | Split user not in group | "Split user is not a member of this group" |
| Add/edit expense | Duplicate split user | "Duplicate users found in the expense splits" |
| Add member | User not found | "User not found" |
| Add member | Already a member | "User is already a member of this group" |
| Remove member | Cannot remove creator | "Cannot remove the group creator" |

Flash messages (success/info) may be adjusted for clarity as long as intent is preserved.
