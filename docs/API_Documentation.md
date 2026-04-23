# API Documentation - CD Web Backend

## Overview
This document describes the RESTful APIs available in the CD Web Backend project.

## Base URL
`http://localhost:8080/api/v1`

## Authentication
JWT-based authentication is used for protected routes.

## Endpoints

### Auth
- `POST /auth/login` - Login to the system
- `POST /auth/register` - Register a new user

### Products
- `GET /products` - List all products
- `GET /products/{id}` - Get product details
