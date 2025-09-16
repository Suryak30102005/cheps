"""
This script implements a Flask web application for Udhyam Sakhi.

It provides the backend for a web-based marketplace, allowing users to
register, login, view products, and place orders. It also includes the
original WhatsApp bot functionality.
"""
from flask import Flask, request, jsonify, render_template, g, flash, redirect, url_for, session
import os
import json
from dotenv import load_dotenv
from datetime import datetime
from twilio.rest import Client
from twilio.base.exceptions import TwilioRestException
import time
import razorpay
import uuid
from werkzeug.security import generate_password_hash, check_password_hash

# Load environment variables
load_dotenv()
TWILIO_ACCOUNT_SID = os.getenv("TWILIO_ACCOUNT_SID")
TWILIO_AUTH_TOKEN = os.getenv("TWILIO_AUTH_TOKEN")
TWILIO_WHATSAPP_NUMBER = os.getenv("TWILIO_WHATSAPP_NUMBER")
VERIFY_TOKEN = os.getenv("VERIFY_TOKEN")
RAZORPAY_KEY_ID = os.getenv("key_id")
RAZORPAY_SECRET = os.getenv("key_secret")

# Initialize Twilio client
client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)

# Initialize Razorpay client
razorpay_client = razorpay.Client(auth=(RAZORPAY_KEY_ID, RAZORPAY_SECRET))

app = Flask(__name__)
app.config['SECRET_KEY'] = os.urandom(24)

# Simple user store
USERS_FILE = 'users.json'

def load_users():
    if not os.path.exists(USERS_FILE):
        return {}
    with open(USERS_FILE, 'r') as f:
        return json.load(f)

def save_users(users):
    with open(USERS_FILE, 'w') as f:
        json.dump(users, f, indent=4)

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

@app.before_request
def before_request():
    g.user = None
    if 'user_id' in session:
        users = load_users()
        g.user = users.get(session['user_id'])


@app.route("/")
def home():
    """
    Renders the home page.
    """
    return render_template("index.html")

@app.route('/register', methods=('GET', 'POST'))
def register():
    if request.method == 'POST':
        name = request.form['name']
        email = request.form['email']
        password = request.form['password']
        role = request.form['role']
        error = None

        if not name:
            error = 'Name is required.'
        elif not email:
            error = 'Email is required.'
        elif not password:
            error = 'Password is required.'

        if error is None:
            users = load_users()
            if email in [user['email'] for user in users.values()]:
                error = f"User {email} is already registered."
            else:
                user_id = str(uuid.uuid4())
                users[user_id] = {
                    'name': name,
                    'email': email,
                    'password': generate_password_hash(password),
                    'role': role
                }
                save_users(users)
                return redirect(url_for("login"))
        flash(error)

    return render_template('register.html')

@app.route('/login', methods=('GET', 'POST'))
def login():
    if request.method == 'POST':
        email = request.form['email']
        password = request.form['password']
        error = None
        users = load_users()
        user = None
        for u in users.values():
            if u['email'] == email:
                user = u
                break

        if user is None:
            error = 'Incorrect email.'
        elif not check_password_hash(user['password'], password):
            error = 'Incorrect password.'

        if error is None:
            session.clear()
            user_id = None
            for uid, u_data in users.items():
                if u_data['email'] == email:
                    user_id = uid
                    break
            session['user_id'] = user_id
            return redirect(url_for('home'))

        flash(error)

    return render_template('login.html')

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('home'))

@app.route('/profile')
def profile():
    return render_template('profile.html')

PRODUCTS_FILE = 'products.json'

def load_products():
    if not os.path.exists(PRODUCTS_FILE):
        return {}
    with open(PRODUCTS_FILE, 'r') as f:
        return json.load(f)

def save_products(products):
    with open(PRODUCTS_FILE, 'w') as f:
        json.dump(products, f, indent=4)

@app.route('/seller/dashboard')
def seller_dashboard():
    if g.user and g.user['role'] == 'seller':
        products = load_products()
        seller_products = {pid: p for pid, p in products.items() if p['seller_id'] == session['user_id']}
        return render_template('seller_dashboard.html', products=seller_products.values())
    return redirect(url_for('login'))

@app.route('/seller/add_product', methods=('GET', 'POST'))
def add_product():
    if g.user and g.user['role'] == 'seller':
        if request.method == 'POST':
            name = request.form['name']
            description = request.form['description']
            price = request.form['price']
            error = None

            if not name:
                error = 'Name is required.'
            elif not description:
                error = 'Description is required.'
            elif not price:
                error = 'Price is required.'

            if error is None:
                products = load_products()
                product_id = str(uuid.uuid4())
                products[product_id] = {
                    'id': product_id,
                    'name': name,
                    'description': description,
                    'price': price,
                    'seller_id': session['user_id']
                }
                save_products(products)
                return redirect(url_for('seller_dashboard'))
            flash(error)
        return render_template('add_product.html')
    return redirect(url_for('login'))

@app.route('/products')
def products():
    products = load_products()
    return render_template('products.html', products=products.values())

@app.route('/cart')
def cart():
    cart_items = []
    total = 0
    if 'cart' in session:
        products = load_products()
        for product_id, quantity in session['cart'].items():
            product = products.get(product_id)
            if product:
                cart_items.append({
                    'id': product_id,
                    'name': product['name'],
                    'price': float(product['price']),
                    'quantity': quantity
                })
                total += float(product['price']) * quantity
    return render_template('cart.html', cart_items=cart_items, total=total)

@app.route('/add_to_cart/<product_id>')
def add_to_cart(product_id):
    if 'cart' not in session:
        session['cart'] = {}
    cart = session['cart']
    cart[product_id] = cart.get(product_id, 0) + 1
    session.modified = True
    flash('Product added to cart!')
    return redirect(url_for('products'))

@app.route('/remove_from_cart/<product_id>')
def remove_from_cart(product_id):
    if 'cart' in session:
        cart = session['cart']
        if product_id in cart:
            del cart[product_id]
            session.modified = True
            flash('Product removed from cart.')
    return redirect(url_for('cart'))

@app.route('/checkout', methods=['GET', 'POST'])
def checkout():
    if 'cart' not in session or not session['cart']:
        return redirect(url_for('products'))

    cart_items = []
    total = 0
    products = load_products()
    for product_id, quantity in session['cart'].items():
        product = products.get(product_id)
        if product:
            cart_items.append({
                'id': product_id,
                'name': product['name'],
                'price': float(product['price']),
                'quantity': quantity
            })
            total += float(product['price']) * quantity

    if request.method == 'POST':
        # This is where you would handle the checkout form submission
        # For now, we'll just redirect to the payment page
        return redirect(url_for('payment'))

    order_amount = int(total * 100)
    order_currency = 'INR'
    order_receipt = str(uuid.uuid4())
    notes = {'name': g.user['name'], 'email': g.user['email']}

    try:
        order = razorpay_client.order.create({
            'amount': order_amount,
            'currency': order_currency,
            'receipt': order_receipt,
            'notes': notes
        })
        order_id = order['id']
        return render_template('checkout.html', cart_items=cart_items, total=total, key_id=RAZORPAY_KEY_ID, order_id=order_id)
    except Exception as e:
        flash(f'Error creating Razorpay order: {e}')
        return redirect(url_for('cart'))


@app.route('/payment_success')
def payment_success():
    payment_id = request.args.get('payment_id')
    order_id = request.args.get('order_id')
    signature = request.args.get('signature')

    params_dict = {
        'razorpay_order_id': order_id,
        'razorpay_payment_id': payment_id,
        'razorpay_signature': signature
    }

    try:
        razorpay_client.utility.verify_payment_signature(params_dict)
        session.pop('cart', None)
        flash('Payment successful!')
        return render_template('payment_success.html')
    except Exception as e:
        flash(f'Payment verification failed: {e}')
        return redirect(url_for('cart'))


@app.route("/webhook", methods=["GET", "POST"])
def webhook():
    """
    Handles incoming webhooks from Twilio.

    GET requests are used for webhook verification.
    POST requests are used for handling incoming messages.

    Returns:
        str: The challenge string for GET requests, or 'OK' for POST requests.
    """
    if request.method == "GET":
        verify_token = request.args.get("hub.verify_token")
        challenge = request.args.get("hub.challenge")
        if verify_token == VERIFY_TOKEN:
            return challenge
        return "Invalid verification token", 403

    elif request.method == "POST":
        data = request.form.to_dict()
        print(f"📩 Incoming Webhook Data: {json.dumps(data, indent=2)}")

        sender = data.get("From")
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
    """
    Sends contact information to the user.

    Args:
        to (str): The recipient's WhatsApp number.
    """
    message = (
        "*📞 Contact Information:*\n\n"
        "👩‍💼 *Owner:* G.Nikhitha\n"
        "📍 *Location:* Tadipatri, Anantapur\n"
        "📱 *Phone:* +91 9392811711\n"
        "📧 *Email:* support@aarticreations.in\n"
        "🕒 *Working Hours:* 10 AM - 6 PM (Mon - Sat)"
    )
    send_message(to, message)

def send_welcome_message(to):
    """
    Sends a welcome message with an image and options to the user.

    Args:
        to (str): The recipient's WhatsApp number.
    """
    image_url = "https://plus.unsplash.com/premium_photo-1679809447923-b3250fb2a0ce?q=80&w=2071&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
    try:
        client.messages.create(
            from_=TWILIO_WHATSAPP_NUMBER,
            media_url=[image_url],
            to=to
        )
        print(f"🖼️ Image sent to {to}")
        time.sleep(1)
    except TwilioRestException as e:
        print(f"❌ Error sending image: {str(e)}")

    message = (
        "Welcome to our small business! We offer handcrafted goods made with love. Reply with:\n"
        "1. View items\n"
        "2. Contact Us"
    )
    send_message(to, message)

def send_menu(to):
    """
    Sends a text-based menu to the user.

    Args:
        to (str): The recipient's WhatsApp number.
    """
    menu_message = (
        "*✨ Our Handmade Collection:*\nSelect items by replying with the number:\n"
        "1. Wool Scarf - ₹450\n"
        "2. Cozy Beanie - ₹350\n"
        "3. Handcrafted Mug - ₹250\n"
        "4. Decorative Bowl - ₹500\n"
        "5. Embroidery Hoop - ₹650"
    )
    send_message(to, menu_message)

def add_to_selection(user_id, item_id):
    """
    Adds a selected item to the user's order.

    Args:
        user_id (str): The user's WhatsApp number.
        item_id (str): The ID of the selected item.
    """
    item_name, item_price = menu_items[item_id]

    if user_id not in user_selections:
        user_selections[user_id] = []

    user_selections[user_id].append((item_name, item_price))
    send_add_more_or_confirm_buttons(user_id)

def send_payment_confirmation(to):
    """
    Sends a payment confirmation prompt to the user.

    Args:
        to (str): The recipient's WhatsApp number.
    """
    send_message(to, "✅ Once paid, reply 'payment_done' to confirm manually (automatic confirmation sent after payment).")

def generate_bill(user_id):
    """
    Generates and sends the bill with a Razorpay payment link.

    Args:
        user_id (str): The user's WhatsApp number.
    """
    if user_id not in user_selections or not user_selections[user_id]:
        send_message(user_id, "*🛒 Your cart is empty!* Please select items from the menu.")
        return

    items = user_selections[user_id]
    total_cost = sum(item[1] for item in items)
    amount_in_paise = total_cost * 100

    order_id = str(uuid.uuid4())[:8]
    reference_map[order_id] = user_id

    bill_message = f"*✨ Your Order Summary:*\nOrder ID: {order_id}\nUserName: umesh\nAddress: Gorantla, Anantapur\n"
    for item_name, item_price in items:
        bill_message += f"- {item_name}: ₹{item_price}\n"
    bill_message += f"\n*Total: ₹{total_cost}*"

    send_message(user_id, bill_message)

    try:
        clean_contact = user_id.replace("whatsapp:", "")
        payment_link_data = {
            "amount": amount_in_paise,
            "currency": "INR",
            "accept_partial": False,
            "reference_id": order_id,
            "description": "Mahila Sakhi Order Payment",
            "customer": {
                "name": "Some User",
                "contact": clean_contact,
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

def send_add_more_or_confirm_buttons(to):
    """
    Sends a text prompt to add more items or confirm the order.

    Args:
        to (str): The recipient's WhatsApp number.
    """
    message = "Do you want to add more items or confirm your order? Reply with:\n1. Add More\n2. Confirm"
    send_message(to, message)

def send_message(to, message):
    """
    Sends a simple text message to the user.

    Args:
        to (str): The recipient's WhatsApp number.
        message (str): The message to send.
    """
    try:
        response = client.messages.create(
            from_=TWILIO_WHATSAPP_NUMBER,
            body=message,
            to=to
        )
        print(f"📤 Sent Message Response: {response.sid}")
        time.sleep(1)
    except TwilioRestException as e:
        print(f"❌ Twilio error: {str(e)}")

def save_bill_to_json(user_id):
    """
    Stores the latest confirmed order in a JSON file and sends it to the seller.

    Args:
        user_id (str): The user's WhatsApp number.
    """
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
    """
    Sends bill details to the seller.

    Args:
        bill_data (dict): A dictionary containing the bill details.
    """
    seller_number = f"whatsapp:+919014056297"
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


@app.route('/payment/webhook', methods=['POST'])
def payment_webhook():
    """
    Handles the Razorpay payment webhook.

    This is called by Razorpay when a payment is successful.

    Returns:
        An empty string with a 200 status code.
    """
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
        username = "Umesh"
        address = "Amaravati, vijayawada"

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

Items:\n""" + "\n".join([f"- {i['name']}: ₹{i['price']}" for i in formatted_items]) + f"\n\n*Total Paid: ₹{amount}*\n✅ Payment Successful."

        send_message(user_id, receipt)
        save_bill_to_json(user_id)
        send_bill_to_seller(bill_data)

    return '', 200

@app.route('/api/latest-order', methods=['GET'])
def get_latest_order():
    """
    Retrieves the latest order from the orders.json file.

    Returns:
        A JSON response with the latest order, or an error message.
    """
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

if __name__ == "__main__":
    app.run(debug=True, port=5000)