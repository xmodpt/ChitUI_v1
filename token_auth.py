"""
Token-Based Authentication Module for ChitUI Mobile API

This module provides JWT token generation and validation for mobile clients.
It works alongside the existing session-based authentication for web browsers.

Features:
- JWT token generation with configurable expiration
- Token validation and refresh
- Token-based decorator for API routes
- Backwards compatible with existing session-based auth

Usage:
    from token_auth import token_required, generate_token

    @app.route('/api/mobile/printers')
    @token_required
    def get_printers():
        return jsonify({"printers": [...]})
"""

import jwt
import datetime
from functools import wraps
from flask import request, jsonify
from loguru import logger


class TokenAuth:
    """JWT Token Authentication Manager"""

    def __init__(self, secret_key, token_expiry_hours=720):
        """
        Initialize Token Authentication

        Args:
            secret_key (str): Secret key for JWT encoding/decoding
            token_expiry_hours (int): Token validity period in hours (default: 720 = 30 days)
        """
        self.secret_key = secret_key
        self.token_expiry_hours = token_expiry_hours
        self.algorithm = 'HS256'

    def generate_token(self, user_id='admin', metadata=None):
        """
        Generate a new JWT token

        Args:
            user_id (str): User identifier (default: 'admin')
            metadata (dict): Optional additional data to include in token

        Returns:
            str: Encoded JWT token
        """
        try:
            payload = {
                'user_id': user_id,
                'iat': datetime.datetime.utcnow(),  # Issued at
                'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=self.token_expiry_hours)
            }

            # Add optional metadata
            if metadata:
                payload.update(metadata)

            token = jwt.encode(payload, self.secret_key, algorithm=self.algorithm)
            logger.info(f"Generated token for user: {user_id}, expires in {self.token_expiry_hours} hours")
            return token

        except Exception as e:
            logger.error(f"Error generating token: {e}")
            return None

    def validate_token(self, token):
        """
        Validate a JWT token

        Args:
            token (str): JWT token to validate

        Returns:
            dict: Decoded token payload if valid, None otherwise
        """
        try:
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            return payload

        except jwt.ExpiredSignatureError:
            logger.warning("Token validation failed: Token has expired")
            return None

        except jwt.InvalidTokenError as e:
            logger.warning(f"Token validation failed: {e}")
            return None

        except Exception as e:
            logger.error(f"Unexpected error validating token: {e}")
            return None

    def refresh_token(self, old_token):
        """
        Refresh an existing token (generate new token with updated expiry)

        Args:
            old_token (str): Current JWT token

        Returns:
            str: New JWT token, or None if old token is invalid
        """
        payload = self.validate_token(old_token)

        if payload:
            # Remove old timestamps
            payload.pop('iat', None)
            payload.pop('exp', None)

            # Generate new token with same data
            user_id = payload.pop('user_id', 'admin')
            return self.generate_token(user_id=user_id, metadata=payload)

        return None

    def get_token_from_request(self):
        """
        Extract JWT token from request headers

        Supports:
        - Authorization: Bearer <token>
        - Authorization: Token <token>
        - X-Auth-Token: <token>

        Returns:
            str: Token string or None
        """
        # Check Authorization header
        auth_header = request.headers.get('Authorization', '')

        if auth_header:
            parts = auth_header.split()
            if len(parts) == 2 and parts[0].lower() in ['bearer', 'token']:
                return parts[1]

        # Check X-Auth-Token header (alternative)
        return request.headers.get('X-Auth-Token')


# Decorator for token-required routes
def create_token_required_decorator(token_auth_instance):
    """
    Factory function to create a token_required decorator

    Args:
        token_auth_instance: Instance of TokenAuth class

    Returns:
        function: Decorator function for protecting routes
    """

    def token_required(f):
        """Decorator to require valid JWT token for API routes"""
        @wraps(f)
        def decorated_function(*args, **kwargs):
            token = token_auth_instance.get_token_from_request()

            if not token:
                logger.warning(f"Token required but not provided for {request.path}")
                return jsonify({
                    'error': 'Authentication token required',
                    'message': 'Please provide a valid token in Authorization header'
                }), 401

            # Validate token
            payload = token_auth_instance.validate_token(token)

            if not payload:
                logger.warning(f"Invalid or expired token for {request.path}")
                return jsonify({
                    'error': 'Invalid or expired token',
                    'message': 'Please login again to get a new token'
                }), 401

            # Token is valid - attach payload to request for use in route
            request.token_payload = payload

            return f(*args, **kwargs)

        return decorated_function

    return token_required
