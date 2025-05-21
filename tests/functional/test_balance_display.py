def test_balance_display_on_group_page(client, app, populated_test_db):
    """Test that balances are displayed on the group page."""
    # Log in the user
    # First login as user1 (who created the apartment group)
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })    
    # Access the group page
    response = client.get('/group/1')
    
    # Check that the balance section is present
    assert b'Balances' in response.data
    
    # Check that the balances are displayed
    # The exact content will depend on your test data
    assert b'owes' in response.data or b'is owed' in response.data