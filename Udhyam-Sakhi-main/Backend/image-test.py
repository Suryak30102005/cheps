from flask import Flask, request, jsonify
import os
import json
from dotenv import load_dotenv
from datetime import datetime
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
        elif message_body == "2":
            send_contact_info(sender)
        elif message_body == "payment_done":
            send_message(sender, "*✅ Payment Confirmed!* Thank you for your order! 🙏")
            save_bill_to_json(sender)
        elif message_body == "confirm":
            generate_bill(sender)
        elif message_body == "add more":
            send_menu(sender)
        elif message_body in menu_items:
            add_to_selection(sender, message_body)
        else:
            send_welcome_message(sender)

        return "OK", 200

def send_contact_info(to):
    """Send contact information as text"""
    message = (
        "*📞 Contact Information:*\n\n"
        "👩‍💼 *Owner:* Gorthi Nikhitha\n"
        "📍 *Location:* Tadipatri, Anantapur\n"
        "📱 *Phone:* +91 9392811711\n"
        "📧 *Email:* support@aarticreations.in\n"
        "🕒 *Working Hours:* 10 AM - 6 PM (Mon - Sat)"
    )
    send_message(to, message)

def send_welcome_message(to):
    """Send welcome message with image and text options"""
    image_url = "https://plus.unsplash.com/premium_photo-1679809447923-b3250fb2a0ce?q=80&w=2071&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
    try:
        client.messages.create(
            from_=TWILIO_WHATSAPP_NUMBER,
            media_url=[image_url],
            to=to
        )
        print(f"🖼️ Image sent to {to}")
        time.sleep(1)  # Avoid rate limiting
    except TwilioRestException as e:
        print(f"❌ Error sending image: {str(e)}")

    message = (
        "Welcome to our small business! We offer handcrafted goods made with love. Reply with:\n"
        "1. View items\n"
        "2. Contact Us"
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

def send_product_cards(to, items):
    """Send product cards with image and text prompt"""
    for item in items:
        try:
            # Send product image
            client.messages.create(
                from_=TWILIO_WHATSAPP_NUMBER,
                media_url=[item["image"]],
                to=to
            )
            print(f"🖼️ Image sent for {item['title']}")
            time.sleep(1)  # Avoid rate limiting

            # Send text prompt
            message = f"*{item['title']}*\n{item['description']}\nReply '{item['id']}' to add to cart."
            send_message(to, message)
            time.sleep(1)
        except TwilioRestException as e:
            print(f"❌ Error sending card for {item['title']}: {str(e)}")

def add_to_selection(user_id, item_id):
    """Add selected item to user's order"""
    item_name, item_price = menu_items[item_id]

    if user_id not in user_selections:
        user_selections[user_id] = []

    user_selections[user_id].append((item_name, item_price))
    send_add_more_or_confirm_buttons(user_id)

def send_payment_confirmation(to):
    """Send payment confirmation prompt"""
    send_message(to, "✅ Once paid, reply 'payment_done' to confirm your payment.")

def generate_bill(user_id):
    """Generate and send the bill with Razorpay payment link"""
    if user_id not in user_selections or not user_selections[user_id]:
        send_message(user_id, "*Your cart is empty!* Please select items from the menu.")
        return

    items = user_selections[user_id]
    total_cost = sum(item[1] for item in items)

    bill_message = f"*✨ Your Order Summary:*\nOrder ID: 867654\nUserName: Umesh\nAddress: Gorantla, Anantapur\n"
    for item_name, item_price in items:
        bill_message += f"- {item_name}: ₹{item_price}\n"
    bill_message += f"\n*Total: ₹{total_cost}*"

    send_message(user_id, bill_message)
    payment_link = "https://razorpay.me/@Umesh"
    send_message(user_id, f"💳 Please make the payment here:\n{payment_link}")
    send_payment_confirmation(user_id)

def send_add_more_or_confirm_buttons(to):
    """Send text prompt to add more items or confirm order"""
    message = "Do you want to add more items or confirm your order? Reply with:\n1. Add More\n2. Confirm"
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

def save_bill_to_json(user_id):
    """Store the latest confirmed order in a JSON file and send to seller"""
    bill_data = {
        "user_id": user_id,
        "username": "umesh",
        "address": "Gorantla, Anantapur",
        "timestamp": datetime.now().isoformat(),
        "items": [],
        "total": 0
    }

    for item_name, item_price in user_selections.get(user_id, []):
        bill_data["items"].append({"name": item_name, "price": item_price})
        bill_data["total"] += item_price

    if not os.path.exists("bills.json"):
        with open("bills.json", "w") as f:
            json.dump([], f)

    with open("bills.json", "r+") as f:
        data = json.load(f)
        data.append(bill_data)
        f.seek(0)
        json.dump(data, f, indent=2)

    send_bill_to_seller(bill_data)

def send_bill_to_seller(bill_data):
    """Send bill details to seller"""
    seller_number = f"whatsapp:+99014056297"
    message = f"🧾 *New Order Received!*\n"
    message += f"👤 Customer: {bill_data['username']}\n"
    message += f"🏠 Address: {bill_data['address']}\n"
    message += f"🕒 Time: {bill_data['timestamp']}\n\n"
    message += "*🛍️ Items:*\n"
    message += "UPI Payment ID - 9392811711\n"
    for item in bill_data["items"]:
        message += f"- {item['name']}: ₹{item['price']}\n"
    message += f"\n💰 *Total: ₹{bill_data['total']}*"

    send_message(seller_number, message)

@app.route("/", methods=["GET"])
def home():
    return jsonify({"status": True, "message": "WhatsApp Bot is Running 🚀"})

if __name__ == "__main__":
    app.run(debug=True, port=5000)