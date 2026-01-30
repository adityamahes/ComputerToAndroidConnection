import socket
import time
import random
import sys

# Configuration
HOST = '127.0.0.1'  # Localhost for adb forwarded port
PORT = 8080             # The port the Android App is listening on

COMMANDS = [
    "UP",
    "DOWN",
    "LEFT",
    "RIGHT",
    "FORWARD",
    "BACKWARD"
]

def send_commands(host, port):
    """
    Connects to the Drone Controller App and sends random commands.
    Simulates a 5Hz stream (command sent every 0.2s).
    """
    print(f"Attempting to connect to {host}:{port}...")
    
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((host, port))
            print(f"Connected to {host}:{port}")
            
            while True:
                # Select a random command
                cmd = random.choice(COMMANDS)
                duration = random.uniform(1.0, 3.0) # Hold for 1-3 seconds
                start_time = time.time()
                
                print(f"Streaming: {cmd} for {duration:.2f}s")
                
                # Send the command at 5Hz (every 0.2s)
                while time.time() - start_time < duration:
                    message = cmd + '\n'
                    s.sendall(message.encode('utf-8'))
                    time.sleep(0.2) # 5Hz
                
                # Pause for Still state
                pause_duration = random.uniform(1.0, 2.0)
                print(f"Still state for {pause_duration:.2f}s")
                time.sleep(pause_duration)
                
    except ConnectionRefusedError:
        print(f"Connection refused. Make sure the app is running on {host} and listening on port {port}.")
    except TimeoutError:
        print(f"Connection timed out. Check IP address and network connectivity.")
    except KeyboardInterrupt:
        print("\nStopping script...")
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    # Allow command line argument for IP
    if len(sys.argv) > 1:
        HOST = sys.argv[1]
    
    print("------------------------------------------------")
    print("Drone Controller TCP Test Script (5Hz Stream)")
    print("------------------------------------------------")
    send_commands(HOST, PORT)
