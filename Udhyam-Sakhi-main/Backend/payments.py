from flask import Flask, request, jsonify
import os
import json
from dotenv import load_dotenv
from twilio.rest import Client
from twilio.base.exceptions import TwilioRestException
import time

# Load environment variables
load_dotenv()
TWILIO_ACCOUNT_SID = os.getenv("TWILIO_ACCOUNT_SID")
TWILIO_AUTH_TOKEN = os.getenv("TWILIO_AUTH_TOKEN")
TWILIO_WHATSAPP_NUMBER = os.getenv("TWILIO_WHATSAPP_NUMBER")  # e.g., whatsapp:+14155238886
VERIFY_TOKEN = os.getenv("VERIFY_TOKEN")

# Initialize Twilio client
client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)

app = Flask(__name__)

# Temporary storage for user selections
user_selections = {}
menu_items = {
    '1': ("Wool Scarf", 450),
    '2': ("Cozy Beanie", 350),
    '3': ("Handcrafted Mug", 250),
    '4': ("Decorative Bowl", 500),
    '5': ("Embroidery Hoop", 650)
}

@app.route("/webhook", methods=["GET", "POST"])
def webhook():
    if request.method == "GET":
        verify_token = request.args.get("hub.verify_token")
        challenge = request.args.get("hub.challenge")
        if verify_token == VERIFY_TOKEN:
            return challenge
        return "Invalid verification token", 403

    elif request.method == "POST":
        data = request.form.to_dict()
        print(f"📩 Incoming Webhook Data: {json.dumps(data, indent=2)}")

        sender = data.get("From")  # e.g., whatsapp:+1234567890
        message_body = data.get("Body", "").lower().strip()

        if message_body == "1":
            send_menu(sender)
        elif message_body == "payment_done":
            send_message(sender, "*✅ Payment Confirmed!*")
            user_selections.pop(sender, None)
        elif message_body == "2":
            generate_bill(sender)
        elif message_body == "add more":
            send_menu(sender)
        elif message_body in menu_items:
            add_to_selection(sender, message_body)
        else:
            send_welcome_message(sender))

        return "OK", 200

def send_welcome_message(to):
    """Send welcome message with text options"""
    message = (
        "Welcome to our small business! We offer handcrafted goods made with love. Reply with:
1. View items
2. Contact Us"
    )
    send_message(to, message)

def send_menu(to):
    """Send a text-based menu"""
    menu_message = (
        "*✨ Our Handmade Collection:*\nSelect items by replying with the number:\n"
        "1. Wool Scarf - ₹450\n"
        "2. Cozy Beanie - ₹350\n"
        "3. Handcrafted Mug - ₹250\n"
        "4. Decorative Bowl - ₹500\n"
        "5. Embroidery Hoop - ₹650"
    )
    send_message(to, menu_message)

def add_to_selection(user_id, sender, item_id):
    """Add selected item to user's order"""
    item_name, item_price = menu_items[item_id]

    if user_id not in user_selections:
        user_selections[user_id] = []

    user_selections[user_id].append((item_name, item_price))
    send_add_more_or_confirm(user_id)

def send_payment_confirmation(to):
    """Send payment confirmation prompt"""
    send_message(to, "✅ Once paid, reply 'payment_done' to confirm your payment.")

def generate_bill(user_id):
    """Generate and send the bill + payment link + confirmation prompt"""
    if user_id not in user_selections or not user_selections[user_id]:
        send_message(user_id, "*Your cart is empty!* Please select items from the menu.")
        return

    items = user_selections[user_id]
    total_cost = sum(item[1] for item in items)

    # Create bill summary
    bill_message = f"*✨ Your Order Summary:*\nOrder ID: 867654\nUserName: umesh\nAddress: Amaravati, Vijayawada\n"
    for item_name, item_price in items:
        bill_message += f"- {item_name}: ₹{item_price}\n"
    bill_message += f"\n*Total: ₹{total_cost}*"

    # Send bill and payment link
    send_message(user_id, bill_message)
    payment_link = "https://rzp.io/rzp/v2R5ZEK"  # Updated to match send_payment_button link
    send_message(user_id, send_message(user_id, f"💳 Please make the payment here:\n{payment_link}"))
    send_payment_confirmation(user_id)

def send_add_more_or_confirm(to):
    """Send text prompt to add more items or confirm order"""
    message = "Do you want to add more items or confirm your order? Reply with:\n1. Add More\n2. Confirm Order"
    send_message(to, message)

def send_message(to, message):
    """Send a simple text message"""
    try:
        response = client.messages.create(
            from_=TWILIO_WHATSAPP_NUMBER,
            body=message,
            to=to
        )
        print(f"📤 Sent Message Response: {response.sid}")
        time.sleep(1)  # Prevent rate limiting
    except TwilioRestException as e:
        print(f"❌ Twilio error: {str(e)}")

@app.route("/", methods=["GET"])
def home():
    return jsonify({"status": True, "message": "WhatsApp Bot is Running 🚀"})

if __name__ == "__main__":
    app.run(debug=True, port=5000)