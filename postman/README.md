# Postman Collections for LiveMenu API

This directory contains Postman collections for testing the LiveMenu API.

## Collections

### 1. `LiveMenu.postman_collection.json` (Complete Collection)
Complete API collection including:
- **Authentication**: Register, Login, Get current user, Logout
- **Restaurant Management (CU-02)**: Get, Create, Update, Delete restaurant

### 2. `Auth-LiveMenu.postman_collection.json` (Auth Only)
Focused collection for authentication endpoints only.

## Setup Instructions

### 1. Import Collection
1. Open Postman
2. Click **Import** button
3. Select the collection file (`LiveMenu.postman_collection.json` or `Auth-LiveMenu.postman_collection.json`)
4. Click **Import**

### 2. Configure Variables
The collection uses the following variables:
- `baseUrl`: Base URL of the API (default: `http://localhost:8080`)
- `access_token`: Automatically set after successful login

To change the base URL:
1. Click on the collection name
2. Go to **Variables** tab
3. Update the `baseUrl` value if needed

### 3. Testing Workflow

#### Authentication Flow
1. **Register**: Create a new user account
   - Update email and password in the request body
   - Expected: 201 Created

2. **Login**: Authenticate and get access token
   - Uses the same email/password from Register
   - The access token is automatically saved to collection variable
   - Expected: 200 OK with `access_token` in response

3. **Get current user**: Verify authentication
   - Uses the saved access token automatically
   - Expected: 200 OK

4. **Logout**: End session
   - Expected: 204 No Content

#### Restaurant Management Flow (CU-02)
**Prerequisites**: Must be authenticated (run Login first)

1. **Create Restaurant**: Create a new restaurant for the authenticated user
   - Update the request body with restaurant details
   - `name` is required (max 100 characters)
   - `description` is optional (max 500 characters)
   - `schedule` is optional JSON object
   - Expected: 201 Created with restaurant data

2. **Get Restaurant**: Retrieve the current user's restaurant
   - Expected: 200 OK with restaurant data, or 404 if not found

3. **Update Restaurant**: Update restaurant information
   - All fields are optional
   - If `name` is updated, `slug` is automatically regenerated
   - Expected: 200 OK with updated restaurant data

4. **Delete Restaurant**: Remove the restaurant
   - Expected: 204 No Content

## Example Request Bodies

### Create Restaurant
```json
{
  "name": "My Restaurant",
  "description": "A great place to eat",
  "logo": "https://example.com/logo.png",
  "phone": "+1234567890",
  "address": "123 Main St, City, Country",
  "schedule": {
    "monday": {
      "open": "09:00",
      "close": "22:00",
      "closed": false
    },
    "tuesday": {
      "open": "09:00",
      "close": "22:00",
      "closed": false
    }
  }
}
```

### Update Restaurant
```json
{
  "name": "Updated Restaurant Name",
  "description": "Updated description",
  "phone": "+1234567891",
  "address": "456 New St, City, Country"
}
```

## Schedule Format

The `schedule` field accepts a JSON object with day names as keys. Each day can have:
- `open`: Opening time (format: "HH:mm")
- `close`: Closing time (format: "HH:mm")
- `closed`: Boolean indicating if the restaurant is closed that day

Example:
```json
{
  "monday": {
    "open": "09:00",
    "close": "22:00",
    "closed": false
  },
  "sunday": {
    "closed": true
  }
}
```

## Error Responses

### Authentication Errors
- **400 Bad Request**: Invalid credentials, email already exists, etc.
- **401 Unauthorized**: Invalid or missing token

### Restaurant Errors
- **400 Bad Request**: Validation errors (e.g., name too long, description too long)
- **401 Unauthorized**: Invalid or missing token
- **404 Not Found**: Restaurant not found for the user

## Notes

- The `access_token` is automatically saved after login and used in subsequent requests
- All restaurant endpoints require authentication
- Each user can only have one restaurant
- The `slug` is automatically generated from the restaurant name and is unique

