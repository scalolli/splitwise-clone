import pytest
from playwright.sync_api import Page, expect

def test_edit_expense_split_behaviour(page: Page):
    base_url = "http://127.0.0.1:5000"  # Adjust if your app runs elsewhere

    # 1. Log in (if required)
    response = page.goto(f"{base_url}/login")
    page.fill('input[name="username"]', 'user1')
    page.fill('input[name="password"]', 'password')
    page.click('button[type="submit"]')

    # 2. Go to the edit expense page (update expense_id as needed)
    expense_id = 1  # Adjust as needed for your test DB
    page.goto(f"{base_url}/expenses/{expense_id}/edit")

    # 3. Remove a split (click the remove button)
    remove_buttons = page.locator('.remove-split')

    print(remove_buttons.count())

    if remove_buttons.count() > 1:
        remove_buttons.nth(1).click()
    else:
        remove_buttons.first.click()

    print("number of remove buttons after click:", remove_buttons.count())

    page.screenshot(path='edit_expense_page.png')  # Take a screenshot for debugging
    # 4. Submit the form (should trigger validation error)
    page.click('button[type="submit"]')
    print(page.content())
    expect(page.locator('.alert-danger')).to_be_visible()

    # # 5. Add a split back (click 'Add Split')
    page.click('#add-split')
    # Fill in the new split fields (adjust selectors as needed)
    split_rows = page.locator('.split-row')
    last_row = split_rows.nth(split_rows.count() - 1)
    last_row.locator('select').select_option('2')  # user2
    last_row.locator('input[type="number"]').fill('50')

    # 6. Submit the form again (should succeed)
    page.click('button[type="submit"]')
    expect(page.locator('.alert-danger')).not_to_be_visible()
    # Optionally, check for a success message or redirect
    expect(page).to_have_url(f"{base_url}/group/1")
