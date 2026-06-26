# Add Customer API

## Create Customer

`POST /api/customers`

Request:

```json
{
  "userName": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "phoneNumber": "9876543210",
  "aadharNumber": "123456789012",
  "panCardNumber": "ABCDE1234F",
  "address": "Chennai",
  "townName": "Tambaram",
  "accountType": "SAVINGS",
  "initialBalance": 500
}
```

Response `201 Created`:

```json
{
  "userId": 1,
  "userName": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "9876543210",
  "aadharNumber": "XXXXXXXX9012",
  "panCardNumber": "XXXXXX234F",
  "address": "Chennai",
  "townName": "Tambaram",
  "countryCode": "US",
  "createdDate": "2026-06-25T14:45:10.123",
  "accounts": [
    {
      "accountId": 101,
      "userId": 1,
      "accountNumber": "21234567890",
      "iban": "US2123456789000000101",
      "balance": 500,
      "accountType": "SAVINGS",
      "currency": "USD",
      "status": "ACTIVE",
      "createdDate": "2026-06-25T14:45:10.125"
    }
  ]
}
```

Validation response example:

```json
{
  "timestamp": "2026-06-25T14:45:10.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed.",
  "validationErrors": {
    "initialBalance": "Initial balance must be at least 100."
  }
}
```

## Get All Customers

`GET /api/customers`

Response `200 OK`:

```json
[
  {
    "userId": 1,
    "userName": "John Doe",
    "email": "john@example.com",
    "phoneNumber": "9876543210",
    "aadharNumber": "123456789012",
    "panCardNumber": "ABCDE1234F",
    "address": "Chennai",
    "townName": "Tambaram",
    "createdDate": "2026-06-25T14:45:10.123",
    "accounts": [
      {
        "accountId": 101,
        "userId": 1,
        "accountNumber": "21234567890",
        "iban": "US2123456789000000101",
        "balance": 500,
        "accountType": "SAVINGS",
        "currency": "USD",
        "status": "ACTIVE",
        "createdDate": "2026-06-25T14:45:10.125"
      }
    ]
  }
]
```

## Get Customer By Id

`GET /api/customers/1`

## Update Customer

`PUT /api/customers/1`

Use the same user fields as create. Account creation happens only on `POST /api/customers`.

## Delete Customer

`DELETE /api/customers/1`

Response: `204 No Content`
