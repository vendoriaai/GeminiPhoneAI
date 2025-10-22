#!/usr/bin/env python3
"""
Gemini API Connection Test
Tests if your API key works with Gemini API
"""

import json
import sys
import time
import ssl
import websocket
from typing import Optional

# ANSI color codes for terminal output
GREEN = '\033[92m'
RED = '\033[91m'
YELLOW = '\033[93m'
BLUE = '\033[94m'
RESET = '\033[0m'
BOLD = '\033[1m'


class GeminiAPITester:
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.ws_url = f"wss://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:bidiGenerateContent?key={api_key}"
        self.ws: Optional[websocket.WebSocketApp] = None
        self.connected = False
        self.test_results = []

    def on_open(self, ws):
        """Handle WebSocket connection open"""
        print(f"{GREEN}✓ WebSocket connected successfully{RESET}")
        self.connected = True
        self.test_results.append(("WebSocket Connection", True, "Connected"))
        
        # Send initial configuration
        config = {
            "setup": {
                "model": {
                    "model": "models/gemini-2.0-flash-exp"
                },
                "generationConfig": {
                    "responseModalities": ["TEXT"],
                    "temperature": 0.7,
                    "maxOutputTokens": 100
                }
            }
        }
        
        print(f"{BLUE}→ Sending configuration...{RESET}")
        ws.send(json.dumps(config))

    def on_message(self, ws, message):
        """Handle incoming messages"""
        try:
            data = json.loads(message)
            
            if "setupComplete" in data:
                print(f"{GREEN}✓ Setup completed successfully{RESET}")
                self.test_results.append(("API Setup", True, "Configuration accepted"))
                
                # Send a test message
                test_msg = {
                    "clientContent": {
                        "turns": [{
                            "role": "user",
                            "parts": [{"text": "Reply with 'Hello, I am working!' if you receive this."}]
                        }],
                        "turnComplete": True
                    }
                }
                print(f"{BLUE}→ Sending test message...{RESET}")
                ws.send(json.dumps(test_msg))
                
            elif "serverContent" in data:
                # Extract response text
                if "modelTurn" in data["serverContent"]:
                    parts = data["serverContent"]["modelTurn"].get("parts", [])
                    for part in parts:
                        if "text" in part:
                            response_text = part["text"]
                            print(f"{GREEN}✓ Received response: {response_text}{RESET}")
                            self.test_results.append(("Message Response", True, response_text[:50]))
                            ws.close()
                            
            elif "error" in data:
                error_msg = data["error"].get("message", "Unknown error")
                print(f"{RED}✗ API Error: {error_msg}{RESET}")
                self.test_results.append(("API Error", False, error_msg))
                ws.close()
                
        except json.JSONDecodeError as e:
            print(f"{YELLOW}⚠ Could not parse message: {e}{RESET}")
        except Exception as e:
            print(f"{RED}✗ Error processing message: {e}{RESET}")

    def on_error(self, ws, error):
        """Handle WebSocket errors"""
        print(f"{RED}✗ WebSocket error: {error}{RESET}")
        self.test_results.append(("WebSocket Error", False, str(error)))

    def on_close(self, ws, close_status_code, close_msg):
        """Handle WebSocket close"""
        if close_status_code:
            print(f"{YELLOW}WebSocket closed: {close_status_code} - {close_msg}{RESET}")
        self.print_summary()

    def test_connection(self):
        """Run the connection test"""
        print(f"\n{BOLD}=== Gemini API Connection Test ==={RESET}\n")
        print(f"API Key: {self.api_key[:10]}...{self.api_key[-4:]}")
        print(f"Endpoint: Gemini 2.0 Flash Experimental\n")
        
        print(f"{BLUE}Starting test...{RESET}\n")
        
        # Create WebSocket connection
        self.ws = websocket.WebSocketApp(
            self.ws_url,
            on_open=self.on_open,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close
        )
        
        # Run WebSocket
        self.ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})

    def print_summary(self):
        """Print test summary"""
        print(f"\n{BOLD}=== Test Summary ==={RESET}\n")
        
        all_passed = True
        for test_name, passed, details in self.test_results:
            status = f"{GREEN}PASS{RESET}" if passed else f"{RED}FAIL{RESET}"
            symbol = "✓" if passed else "✗"
            print(f"{symbol} {test_name}: {status}")
            if details and not passed:
                print(f"  {YELLOW}→ {details}{RESET}")
            if not passed:
                all_passed = False
        
        print(f"\n{BOLD}Overall Result: ", end="")
        if all_passed and len(self.test_results) >= 2:
            print(f"{GREEN}✓ ALL TESTS PASSED{RESET}")
            print(f"\nYour Gemini API key is working correctly!")
            print(f"The app should be able to connect and process calls.")
        else:
            print(f"{RED}✗ TESTS FAILED{RESET}")
            print(f"\nPlease check:")
            print(f"1. Your API key is correct")
            print(f"2. The Gemini API is enabled in your Google Cloud project")
            print(f"3. You have sufficient quota remaining")
            print(f"4. Your network allows WebSocket connections")


def read_api_key():
    """Read API key from gradle.properties or user input"""
    import os
    
    # Try to read from gradle.properties
    gradle_props = "gradle.properties"
    if os.path.exists(gradle_props):
        with open(gradle_props, 'r') as f:
            for line in f:
                if line.startswith('GEMINI_API_KEY='):
                    api_key = line.split('=', 1)[1].strip()
                    if api_key and api_key != 'YOUR_GEMINI_API_KEY_HERE':
                        return api_key
    
    # Ask user for API key
    print(f"{YELLOW}API key not found in gradle.properties{RESET}")
    api_key = input("Please enter your Gemini API key: ").strip()
    return api_key


def main():
    """Main test function"""
    print(f"{BOLD}{BLUE}Gemini Phone AI - API Connection Tester{RESET}")
    print("=" * 50)
    
    # Check for required module
    try:
        import websocket
    except ImportError:
        print(f"{RED}Error: websocket-client module not installed{RESET}")
        print(f"Install it with: {YELLOW}pip install websocket-client{RESET}")
        sys.exit(1)
    
    # Get API key
    api_key = read_api_key()
    
    if not api_key:
        print(f"{RED}Error: No API key provided{RESET}")
        sys.exit(1)
    
    # Run test
    tester = GeminiAPITester(api_key)
    
    try:
        tester.test_connection()
    except KeyboardInterrupt:
        print(f"\n{YELLOW}Test interrupted by user{RESET}")
    except Exception as e:
        print(f"\n{RED}Test failed with error: {e}{RESET}")
        sys.exit(1)


if __name__ == "__main__":
    main()
