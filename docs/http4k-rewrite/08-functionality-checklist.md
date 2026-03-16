# Functionality Checklist

This document captures the application behavior that the Kotlin/http4k implementation
must deliver. It replaces the need to keep the old implementation in the repository.

---

## Public pages

- `GET /` returns `200 OK`
- Home page lists all groups
- Home page lists all registered users
- Logged-in users can navigate to group creation

---

## Authentication

- `GET /register` renders the registration form
- `POST /register` validates required fields, password confirmation, unique username,
  and unique email
- Successful registration redirects to `/login`
- `GET /login` renders the login form
- `POST /login` accepts valid credentials and establishes a session cookie
- Invalid login shows `Invalid username or password`
- `GET /logout` clears the session and redirects to `/`

---

## Session enforcement

- Public routes remain accessible without login
- Protected routes redirect unauthenticated users to `/login`
- Authorization is enforced per route purpose, not only by authentication

---

## Groups

- Authenticated users can view a group detail page
- Group detail shows name, description, members, expenses, and balances
- Authenticated users can create a group
- Group creator becomes the first member automatically
- Group creator can edit name and description
- Group creator can add members by username
- Group creator can remove members except the creator
- Missing groups return `404`

---

## Expenses

- Group members can open the add-expense form
- Group members can submit an expense with one or more split rows
- Add/edit expense validation covers:
  - non-empty description
  - total amount greater than zero
  - payer is a group member
  - all split users are group members
  - no duplicate split users
  - payer included in splits
  - split totals equal the expense total within the allowed tolerance
- Expense payer or group creator can edit an expense
- Expense payer or group creator can delete an expense
- Missing expenses return `404`

---

## Balances

- Balances are derived from recorded expenses and shares
- Reciprocal debts are netted into a single displayed balance per pair
- Fully settled pairs are omitted from the displayed balances
- If no balances remain, the UI shows the settled-up state

---

## Settlements

- Group members can record a settlement between two different members
- Settlement amount must be greater than zero
- Both settlement users must belong to the group
- Settlement history is visible to group members
- Settlement history is ordered by date descending
- Recorded settlements reduce displayed balances

---

## User-visible rules to preserve

- Validation errors are shown inline on forms
- Successful mutations redirect back to the relevant page with a success message
- Authorization failures return a redirect or forbidden response appropriate to the route
- Money uses decimal-safe arithmetic throughout the domain and persistence layers

---

## Implementation gaps to ensure are covered

- Centralized protected-route session filter
- Expense edit authorization
- Expense deletion
- Settlement recording and settlement history HTTP flows
- Balance calculation that includes settlements
