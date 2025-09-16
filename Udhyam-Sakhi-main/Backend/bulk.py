from flask import Flask, request, jsonify
import os
import json
from dotenv import load_dotenv
from datetime import datetime
from twilio.rest import Client
from twilio.base.exceptions import TwilioRestException
import time
import razorpay
import uuid
from collections import defaultdict

# Load environment variables
load_dotenv()
TWILIO_ACCOUNT_SID = os.getenv("TWILIO_ACCOUNT_SID")
TWILIO_AUTH_TOKEN = os.getenv("TWILIO_AUTH_TOKEN")
TWILIO_WHATSAPP_NUMBER = os.getenv("TWILIO_WHATSAPP_NUMBER")  # e.g., whatsapp:+14155238886
VERIFY_TOKEN = os.getenv("VERIFY_TOKEN")
RAZORPAY_KEY_ID = os.getenv("key_id")
RAZORPAY_SECRET = os.getenv("key_secret")

# Initialize Twilio client
client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)

# Initialize Razorpay client
razorpay_client = razorpay.Client(auth=(RAZORPAY_KEY_ID, RAZORPAY_SECRET))

app = Flask(__name__)

# Temporary storage for user selections and reference map
user_selections = {}
reference_map = {}
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
        "👩‍💼 *Owner:* Nikhitha\n"
        "📍 *Location:* Tadipatri, Anantapur\n"
        "📱 *Phone:* +91 9014056297\n"
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
    send_message(to, "✅ Once paid, reply 'payment_done' to confirm manually (automatic confirmation sent after payment).")

def generate_bill(user_id):
    """Generate and send the bill with Razorpay payment link for single orders"""
    if user_id not in user_selections or not user_selections[user_id]:
        send_message(user_id, "*🛒 Your cart is empty!* Please select items from the menu.")
        return

    items = user_selections[user_id]
    total_cost = sum(item[1] for item in items)
    amount_in_paise = total_cost * 100

    order_id = str(uuid.uuid4())[:8]
    reference_map[order_id] = user_id

    bill_message = f"*✨ Your Order Summary:*\nOrder ID: {order_id}\nUserName: umesh\nAddress: Bhupeshnagar, Nagpur\n"
    for item_name, item_price in items:
        bill_message += f"- {item_name}: ₹{item_price}\n"
    bill_message += f"\n*Total: ₹{total_cost}*"

    send_message(user_id, bill_message)

    try:
        payment_link_data = {
            "amount": amount_in_paise,
            "currency": "INR",
            "accept_partial": False,
            "reference_id": order_id,
            "description": "Mahila Sakhi Order Payment",
            "customer": {
                "name": "Some User",
                "contact": user_id,
                "email": "demo@example.com"
            },
            "notify": {
                "sms": False,
                "email": False
            },
            "callback_url": "https://397c-103-217-237-56.ngrok-free.app/payment/webhook"
        }

        response = razorpay_client.payment_link.create(payment_link_data)
        payment_link = response['short_url']

        send_message(user_id, f"💳 Please make the payment here:\n{payment_link}")
        send_payment_confirmation(user_id)

    except Exception as e:
        print(f"❌ Razorpay error: {str(e)}")
        send_message(user_id, "⚠️ Failed to generate payment link. Please try again later.")

def generate_bulk_bill(user_id, orders):
    """Generate and send a bulk bill with Razorpay payment link for advance payment"""
    # Override user_id for bulk orders
    user_id = "whatsapp:+919392811711"

    # Calculate total cost
    total_cost = sum(order['total'] for order in orders)
    advanced = 0.1 * total_cost
    amount_in_paise = int(advanced * 100)

    # Create unique order ID
    order_id = str(uuid.uuid4())[:8]
    reference_map[order_id] = user_id

    # Build bill message
    bill_message = f"*📦 Bulk Order Summary:*\n🆔 Order ID: {order_id}\n👤 User: {user_id}\n"
    for idx, order in enumerate(orders, 1):
        bill_message += f"\n🔢 Order {idx}:\n"
        for item in order['items']:
            name = item['name']
            quantity = item['quantity']
            price = item['price']
            item_total = price * quantity
            bill_message += f"- {name} (x{quantity}): ₹{item_total}\n"
    bill_message += f"\n💰 Total Cost: ₹{total_cost}\n"
    bill_message += f"*Total Payable in Advance (10%): ₹{advanced}*"

    send_message(user_id, bill_message)

    # Razorpay Payment Link
    try:
        payment_link_data = {
            clean_contact = user_id.replace("whatsapp:", "")
            "amount": amount_in_paise,
            "currency": "INR",
            "accept_partial": False,
            "reference_id": order_id,
            "description": f"Bulk Order - {user_id}",
            "customer": {
                "name": "umesh",
                "contact": clean_contact,
                "email": "demo@example.com"
            },
            "notify": {
                "sms": False,
                "email": False
            },
            "callback_url": "https://2ef8-103-217-237-56.ngrok-free.app/payment/webhook"
        }

        response = razorpay_client.payment_link.create(payment_link_data)
        payment_link = response['short_url']

        send_message(
            user_id,
            f"💳 *Payment Link:* {payment_link}\n\n"
            "✅ Please complete the advance payment (10% of the total). "
            "Remaining payment will be due on delivery. "
            "You'll get an auto-confirmation once done."
        )

    except Exception as e:
        print(f"❌ Razorpay error: {str(e)}")
        send_message(user_id, "⚠️ Failed to generate payment link. Please try again later.")

def send_add_more_or_confirm_buttons(to):
    """Send text prompt to add more items or confirm user"""
    message = "Do you want to add more items or user? Reply with:\n1. Add More\n2. Confirm"
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
        "address": "Bhupeshnagar, Nagpur",
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
    seller_number = f"whatsapp:+919730182225"
    message = f"🧾 *New Order Received!*\n"
    message += f"👤 Customer: {bill_data['username']}\n"
    message += f"🏠 Address: {bill_data['address']}\n"
    message += f"🕒 Time: {bill_data['timestamp']}\n\n"
    message += "*🛍️ Items:*\n"
    message += f"🧾 UPI Payment ID: {bill_data.get('payment_id', 'N/A')}\n\n"
    message += "Order to be prepared in 5 days\n"
    for item in bill_data["items"]:
        message += f"- {item['name']}: ₹{item['price']}\n"
    message += f"\n💰 *Total: ₹{bill_data['total']}*"

    send_message(seller_number, message)

def send_bulk_orders_to_seller(bulk_orders_data):
    """Send bulk order details to seller and generate bills"""
    seller_number = f"whatsapp:+919730182225"
    message = f"📦 *New Bulk Orders Received!*\n\n"

    for idx, order in enumerate(bulk_orders_data, 1):
        user_id = order.get('user_id') or order.get('username')
        order['user_id'] = user_id

        message += f"🔢 Order {idx}\n"
        message += f"👤 User ID: {user_id}\n"
        message += f"🏠 Address: {order['address']}\n"
        message += f"🕒 Time: {order['timestamp']}\n"
        message += f"🧾 UPI Payment ID: {order.get('payment_id', 'N/A')}\n"
        message += "*🛍️ Items:*\n"

        # Set user cart for generate_bill
        user_selections[user_id] = []
        for item in order["items"]:
            item_total = item["price"] * item["quantity"]
            message += f"- {item['name']} (x{item['quantity']}): ₹{item_total}\n"
            user_selections[user_id].append((item["name"], item_total))

        message += f"💰 *Total: ₹{order['total']}*\n"
        message += "⏳ Order to be prepared in 5 days\n"
        message += "-----------------------------\n\n"

    send_message(seller_number, message)

@app.route("/", methods=["GET"])
def home():
    return jsonify({"status": True, "message": "WhatsApp Bot is Running 🚀"})

@app.route('/payment/webhook', methods=['POST'])
def payment_webhook():
    """Handle Razorpay payment webhook"""
    data = request.json
    print("📩 Incoming Webhook Data:", data)

    event = data.get('event')
    payload = data.get('payload', {})

    if event == "payment_link.paid":
        payment_info = payload.get("payment_link", {}).get("entity", {})
        reference_id = payment_info.get("reference_id")
        payment_id = payment_info.get("id")
        amount = int(payment_info.get("amount", 0)) // 100

        user_id = reference_map.get(reference_id)
        if not user_id:
            print("⚠️ No matching user found for reference_id:", reference_id)
            return '', 200

        items = user_selections.get(user_id, [])
        if not items:
            send_message(user_id, "⚠️ Your order could not be found after payment.")
            return '', 200

        now = datetime.now().strftime("%d-%m-%Y %H:%M:%S")
        username = "umesh"
        address = "Bhupeshnagar, Nagpur"

        formatted_items = [{"name": name, "price": price} for name, price in items]

        bill_data = {
            "username": username,
            "address": address,
            "timestamp": now,
            "items": formatted_items,
            "total": amount,
            "payment_id": payment_id
        }

        with open("orders.json", "a") as f:
            f.write(json.dumps(bill_data) + "\n")

        user_selections[user_id] = []

        receipt = f"""🧾 *Mahila Udyam - Order Receipt*\n
Order ID: {reference_id}
Payment ID: {payment_id}
Date: {now}

Items:\n""" + "\n".join([f"- {i['name']}: ₹{i['price']}" for i in formatted_items]) + f"\n\n*Total Paid: ₹{amount}*\n✅ Payment Successful."""

        send_message(user_id, receipt)
        save_bill_to_json(user_id)
        send_bill_to_seller(bill_data)

    return '', 200

@app.route('/api/latest-order', methods=['GET'])
def get_latest_order():
    """Retrieve the latest order from orders.json"""
    latest_file = "orders.json"

    if not os.path.exists(latest_file):
        return jsonify({"error": "orders.json file not found"}), 404

    try:
        with open(latest_file, "r") as f:
            lines = [line.strip() for line in f if line.strip()]

        if not lines:
            return jsonify({"error": "No orders found"}), 404

        latest_order = json.loads(lines[-1])
        return jsonify(latest_order), 200

    except json.JSONDecodeError as e:
        return jsonify({"error": f"Invalid JSON format: {str(e)}"}), 500

    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/bulk', methods=['POST'])
def add_bulk_orders():
    """Add bulk orders to orders.json and notify seller"""
    orders = request.get_json()

    if not isinstance(orders, list):
        return jsonify({"error": "Payload should be a list of orders"}), 400

    try:
        # Replace 'username' with 'user_id' in each order
        for order in orders:
            order['user_id'] = order.pop('username', order.get('user_id'))

        # Save orders to JSONL file
        with open("orders.json", "a") as f:
            for order in orders:
                json.dump(order, f)
                f.write("\n")

        # Group orders by user_id
        grouped_orders = defaultdict(list)
        for order in orders:
            grouped_orders[order['user_id']].append(order)

        # Send all orders to seller
        send_bulk_orders_to_seller(orders)

        # Generate one bill and Razorpay link per user
        for user_id, user_orders in grouped_orders.items():
            generate_bulk_bill(user_id, user_orders)

        return jsonify({"message": f"{len(orders)} orders added, billed, and sent to seller."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(debug=True, port=5000)