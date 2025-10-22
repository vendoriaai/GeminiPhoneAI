#!/usr/bin/env python3
"""
Simple Gemini API Test - Tests REST API endpoint
"""

import requests
import json
import os

def test_gemini_rest_api():
    """Test Gemini API using REST endpoint"""
    
    # Read API key from gradle.properties
    api_key = None
    if os.path.exists('gradle.properties'):
        with open('gradle.properties', 'r') as f:
            for line in f:
                if line.startswith('GEMINI_API_KEY='):
                    api_key = line.split('=', 1)[1].strip()
                    break
    
    if not api_key:
        print("❌ API key not found in gradle.properties")
        return False
    
    print(f"Testing API key: {api_key[:10]}...{api_key[-4:]}")
    print()
    
    # Test with REST API (simpler than WebSocket)
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key={api_key}"
    
    payload = {
        "contents": [{
            "parts": [{
                "text": "Say 'API is working' if you receive this."
            }]
        }]
    }
    
    print("🔄 Sending test request to Gemini API...")
    
    try:
        response = requests.post(url, json=payload, timeout=10)
        
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            if 'candidates' in data:
                text = data['candidates'][0]['content']['parts'][0]['text']
                print(f"✅ SUCCESS! API Response: {text}")
                print()
                print("✅ Your Gemini API key is working correctly!")
                print("✅ The app should be able to connect to Gemini.")
                return True
        elif response.status_code == 403:
            print("❌ 403 Forbidden Error")
            print()
            print("Possible causes:")
            print("1. API key is invalid or expired")
            print("2. Gemini API is not enabled in your Google Cloud project")
            print("3. API key doesn't have permission for this API")
            print()
            print("To fix:")
            print("1. Go to https://aistudio.google.com/app/apikey")
            print("2. Create a new API key")
            print("3. Update gradle.properties with the new key")
        elif response.status_code == 429:
            print("❌ 429 Rate Limit Error")
            print("You've exceeded the API quota. Wait a bit and try again.")
        else:
            print(f"❌ Error: {response.status_code}")
            print(response.text[:500])
        
        return False
        
    except requests.exceptions.RequestException as e:
        print(f"❌ Network Error: {e}")
        print()
        print("Check your internet connection.")
        return False

if __name__ == "__main__":
    print("=" * 60)
    print("Gemini API Simple Test")
    print("=" * 60)
    print()
    
    success = test_gemini_rest_api()
    
    print()
    print("=" * 60)
    if success:
        print("✅ TEST PASSED - API is working!")
    else:
        print("❌ TEST FAILED - API connection issue")
    print("=" * 60)
