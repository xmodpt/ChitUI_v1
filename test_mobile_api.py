#!/usr/bin/env python3
"""
Test script for ChitUI Mobile API endpoints

This script tests the token authentication system and mobile API endpoints.
Run this to verify that everything is working correctly.

Usage:
    python3 test_mobile_api.py
"""

import requests
import json
import sys

# Configuration
BASE_URL = "http://localhost:8080"
PASSWORD = "admin"  # Default password

def print_test(name):
    """Print test header"""
    print(f"\n{'='*60}")
    print(f"TEST: {name}")
    print('='*60)

def print_result(success, message):
    """Print test result"""
    status = "✓ PASS" if success else "✗ FAIL"
    print(f"{status}: {message}")

def test_mobile_login():
    """Test mobile login endpoint"""
    print_test("Mobile Login")

    try:
        response = requests.post(
            f"{BASE_URL}/api/mobile/login",
            json={"password": PASSWORD},
            timeout=5
        )

        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        if response.status_code == 200:
            data = response.json()
            if data.get('success') and data.get('token'):
                print_result(True, "Login successful, token received")
                return data.get('token')
            else:
                print_result(False, "Login failed: " + data.get('message', 'Unknown error'))
                return None
        else:
            print_result(False, f"HTTP {response.status_code}")
            return None

    except requests.exceptions.ConnectionError:
        print_result(False, "Cannot connect to ChitUI - is it running?")
        return None
    except Exception as e:
        print_result(False, f"Error: {e}")
        return None

def test_mobile_login_wrong_password():
    """Test mobile login with wrong password"""
    print_test("Mobile Login - Wrong Password")

    try:
        response = requests.post(
            f"{BASE_URL}/api/mobile/login",
            json={"password": "wrongpassword"},
            timeout=5
        )

        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        if response.status_code == 401:
            print_result(True, "Correctly rejected wrong password")
        else:
            print_result(False, f"Expected 401, got {response.status_code}")

    except Exception as e:
        print_result(False, f"Error: {e}")

def test_get_printers(token):
    """Test get printers endpoint"""
    print_test("Get Printers")

    if not token:
        print_result(False, "No token available - skipping test")
        return

    try:
        response = requests.get(
            f"{BASE_URL}/api/mobile/printers",
            headers={"Authorization": f"Bearer {token}"},
            timeout=5
        )

        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        if response.status_code == 200:
            data = response.json()
            if data.get('success'):
                printer_count = data.get('count', 0)
                print_result(True, f"Retrieved {printer_count} printers")
            else:
                print_result(False, "Request failed: " + data.get('message', 'Unknown error'))
        else:
            print_result(False, f"HTTP {response.status_code}")

    except Exception as e:
        print_result(False, f"Error: {e}")

def test_get_status(token):
    """Test get status endpoint"""
    print_test("Get System Status")

    if not token:
        print_result(False, "No token available - skipping test")
        return

    try:
        response = requests.get(
            f"{BASE_URL}/api/mobile/status",
            headers={"Authorization": f"Bearer {token}"},
            timeout=5
        )

        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        if response.status_code == 200:
            data = response.json()
            if data.get('success'):
                print_result(True, "Status retrieved successfully")
            else:
                print_result(False, "Request failed")
        else:
            print_result(False, f"HTTP {response.status_code}")

    except Exception as e:
        print_result(False, f"Error: {e}")

def test_refresh_token(token):
    """Test token refresh endpoint"""
    print_test("Refresh Token")

    if not token:
        print_result(False, "No token available - skipping test")
        return None

    try:
        response = requests.post(
            f"{BASE_URL}/api/mobile/refresh-token",
            headers={"Authorization": f"Bearer {token}"},
            timeout=5
        )

        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        if response.status_code == 200:
            data = response.json()
            if data.get('success') and data.get('token'):
                print_result(True, "Token refreshed successfully")
                return data.get('token')
            else:
                print_result(False, "Refresh failed")
                return None
        else:
            print_result(False, f"HTTP {response.status_code}")
            return None

    except Exception as e:
        print_result(False, f"Error: {e}")
        return None

def test_unauthorized_access():
    """Test accessing protected endpoint without token"""
    print_test("Unauthorized Access (No Token)")

    try:
        response = requests.get(
            f"{BASE_URL}/api/mobile/printers",
            timeout=5
        )

        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        if response.status_code == 401:
            print_result(True, "Correctly rejected request without token")
        else:
            print_result(False, f"Expected 401, got {response.status_code}")

    except Exception as e:
        print_result(False, f"Error: {e}")

def test_invalid_token():
    """Test accessing protected endpoint with invalid token"""
    print_test("Invalid Token")

    try:
        response = requests.get(
            f"{BASE_URL}/api/mobile/printers",
            headers={"Authorization": "Bearer invalid_token_here"},
            timeout=5
        )

        print(f"Status Code: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        if response.status_code == 401:
            print_result(True, "Correctly rejected invalid token")
        else:
            print_result(False, f"Expected 401, got {response.status_code}")

    except Exception as e:
        print_result(False, f"Error: {e}")

def main():
    """Run all tests"""
    print("\n" + "="*60)
    print("ChitUI Mobile API Test Suite")
    print("="*60)
    print(f"Base URL: {BASE_URL}")
    print(f"Testing with password: {PASSWORD}")

    # Test 1: Successful login
    token = test_mobile_login()

    # Test 2: Failed login (wrong password)
    test_mobile_login_wrong_password()

    # Test 3: Unauthorized access (no token)
    test_unauthorized_access()

    # Test 4: Invalid token
    test_invalid_token()

    if token:
        # Test 5: Get printers (with valid token)
        test_get_printers(token)

        # Test 6: Get system status (with valid token)
        test_get_status(token)

        # Test 7: Refresh token
        new_token = test_refresh_token(token)

        if new_token:
            # Test 8: Use refreshed token
            print_test("Using Refreshed Token")
            test_get_printers(new_token)

    # Summary
    print("\n" + "="*60)
    print("Test Suite Complete")
    print("="*60)
    print("\nIf all tests passed, your mobile API is ready to use!")
    print("You can now start developing your Android app.")
    print("\n")

if __name__ == "__main__":
    main()
