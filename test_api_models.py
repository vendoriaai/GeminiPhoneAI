#!/usr/bin/env python3
"""
Test different Gemini models to find which one works
"""

import requests
import json
import os

def get_api_key():
    """Read API key from gradle.properties"""
    if os.path.exists('gradle.properties'):
        with open('gradle.properties', 'r') as f:
            for line in f:
                if line.startswith('GEMINI_API_KEY='):
                    return line.split('=', 1)[1].strip()
    return None

def test_model(model_name, api_key):
    """Test a specific Gemini model"""
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={api_key}"
    
    payload = {
        "contents": [{
            "parts": [{
                "text": "Hello, respond with 'Working!' if you receive this."
            }]
        }]
    }
    
    try:
        response = requests.post(url, json=payload, timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            if 'candidates' in data:
                text = data['candidates'][0]['content']['parts'][0]['text']
                return True, text
            return False, "No response text"
        else:
            return False, f"Status {response.status_code}: {response.text[:200]}"
            
    except Exception as e:
        return False, str(e)

def main():
    print("=" * 70)
    print("Gemini API Model Compatibility Test")
    print("=" * 70)
    print()
    
    api_key = get_api_key()
    if not api_key:
        print("❌ API key not found in gradle.properties")
        return
    
    print(f"Testing API key: {api_key[:10]}...{api_key[-4:]}")
    print()
    
    # Models to test
    models = [
        "gemini-pro",
        "gemini-1.5-pro",
        "gemini-1.5-flash",
        "gemini-2.0-flash-exp",
    ]
    
    working_models = []
    
    for model in models:
        print(f"Testing {model}...", end=" ")
        success, result = test_model(model, api_key)
        
        if success:
            print(f"✅ WORKS! Response: {result[:50]}")
            working_models.append(model)
        else:
            print(f"❌ FAILED")
            if "404" in result:
                print(f"   → Model not found or not accessible")
            elif "403" in result:
                print(f"   → Permission denied")
            else:
                print(f"   → {result[:100]}")
        print()
    
    print("=" * 70)
    print("Summary:")
    print("=" * 70)
    
    if working_models:
        print(f"✅ {len(working_models)} model(s) working:")
        for model in working_models:
            print(f"   • {model}")
        print()
        print("✅ Your API key is valid!")
        print()
        if "gemini-2.0-flash-exp" not in working_models:
            print("⚠️  Note: gemini-2.0-flash-exp is not accessible")
            print("   You may need to use a different model in the app")
            print(f"   Recommended: {working_models[0]}")
    else:
        print("❌ No models are working with this API key")
        print()
        print("Possible issues:")
        print("1. API key is invalid")
        print("2. Generative Language API is not enabled")
        print("3. API key has restrictions")
        print()
        print("Solutions:")
        print("1. Visit: https://aistudio.google.com/app/apikey")
        print("2. Create a new API key")
        print("3. Make sure 'Generative Language API' is enabled")
        print("4. Update gradle.properties with the new key")

if __name__ == "__main__":
    main()
