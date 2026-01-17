#!/usr/bin/env python3
"""
ONVIF Camera Test Script
Tests ONVIF connectivity and authentication with your camera
"""

import requests
import hashlib
import base64
import uuid
from datetime import datetime
import xml.etree.ElementTree as ET

def create_password_digest(nonce, created, password):
    """Create WS-Security password digest"""
    digest_input = nonce + created + password
    sha1_hash = hashlib.sha1(digest_input.encode()).digest()
    return base64.b64encode(sha1_hash).decode()

def create_soap_request(method, username=None, password=None):
    """Create SOAP request with optional WS-Security"""
    
    # Create WS-Security header if credentials provided
    security_header = ""
    if username and password:
        created = datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%S.%fZ')
        nonce = str(uuid.uuid4()).replace('-', '')[:16]
        digest = create_password_digest(nonce, created, password)
        
        security_header = f"""
        <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
            <wsse:UsernameToken>
                <wsse:Username>{username}</wsse:Username>
                <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">{digest}</wsse:Password>
                <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">{base64.b64encode(nonce.encode()).decode()}</wsse:Nonce>
                <wsu:Created xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">{created}</wsu:Created>
            </wsse:UsernameToken>
        </wsse:Security>
        """
    
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:tds="http://www.onvif.org/ver10/device/wsdl">
    <soap:Header>
        {security_header}
    </soap:Header>
    <soap:Body>
        <tds:{method}/>
    </soap:Body>
</soap:Envelope>"""

def test_onvif_endpoint(url, username=None, password=None):
    """Test ONVIF endpoint with optional credentials"""
    print(f"Testing: {url}")
    print(f"Username: {username if username else 'None'}")
    print(f"Password: {'***' if password else 'None'}")
    
    try:
        soap_request = create_soap_request("GetDeviceInformation", username, password)
        
        headers = {
            'Content-Type': 'application/soap+xml; charset=utf-8',
            'SOAPAction': ''
        }
        
        response = requests.post(url, data=soap_request, headers=headers, timeout=10)
        
        print(f"Response Code: {response.status_code}")
        print(f"Response Headers: {dict(response.headers)}")
        
        if response.status_code == 200:
            print("✅ SUCCESS! ONVIF request worked")
            print(f"Response: {response.text[:500]}...")
            return True
        elif response.status_code == 401:
            print("❌ 401 Unauthorized - Wrong credentials")
        elif response.status_code == 404:
            print("❌ 404 Not Found - Wrong ONVIF endpoint")
        else:
            print(f"❌ HTTP {response.status_code}")
            print(f"Response: {response.text[:200]}...")
            
    except requests.exceptions.ConnectTimeout:
        print("❌ Connection timeout")
    except requests.exceptions.ConnectionError as e:
        print(f"❌ Connection error: {e}")
    except Exception as e:
        print(f"❌ Error: {e}")
    
    return False

def main():
    print("=== ONVIF Camera Test ===")
    
    # Test different endpoints and credentials
    endpoints = [
        "http://10.0.0.51:80/onvif/device_service",
        "http://10.0.0.51:80/onvif/services",
        "http://10.0.0.51:80/onvif",
        "http://10.0.0.51/onvif/device_service",
        "http://10.0.0.51/onvif",
        "http://camera2.retallack.org.uk:80/onvif/device_service",
        "http://camera2.retallack.org.uk/onvif/device_service",
    ]
    
    credentials = [
        (None, None),  # No auth
        ("thingino", "AjVHVSUPOoZQkGG2U"),  # Your ONVIF credentials
        ("admin", "admin"),  # Common defaults
        ("admin", "password"),
        ("admin", ""),
        ("", ""),
    ]
    
    print("\n=== Testing Endpoints ===")
    for endpoint in endpoints:
        print(f"\n--- Testing {endpoint} ---")
        
        for username, password in credentials:
            print(f"\nTrying: {username if username else 'no-auth'}")
            if test_onvif_endpoint(endpoint, username, password):
                print(f"🎉 FOUND WORKING COMBINATION!")
                print(f"URL: {endpoint}")
                print(f"Username: {username}")
                print(f"Password: {password}")
                return
            print()
    
    print("\n❌ No working ONVIF configuration found")
    print("Your camera may not support ONVIF or uses different authentication")

if __name__ == "__main__":
    main()
