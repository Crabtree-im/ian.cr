"""
Beginner assignment: coupon cart splitter

Rules:
1) One coupon per order.
2) A coupon can only be used once.
3) Shipping is free when order subtotal is at least $100.
4) If subtotal is under $100, shipping fee is charged.

Goal:
Split items into multiple orders to maximize:

	net_savings = total_coupon_discount - total_shipping_fees
"""


def order_total(items, order_indexes):
	total = 0.0
	for i in order_indexes:
		total += items[i]["price"]
	return round(total, 2)


def evaluate_orders(items, orders, coupons, shipping_threshold, shipping_fee):
	"""
	orders is a list of 4 buckets:
	orders[0] = no-coupon order
	orders[1] = order for coupon 0
	orders[2] = order for coupon 1
	orders[3] = order for coupon 2
	"""
	total_discount = 0.0
	total_shipping = 0.0
	order_details = []

	for bucket_index in range(4):
		if len(orders[bucket_index]) == 0:
			continue

		subtotal = order_total(items, orders[bucket_index])
		shipping = 0.0 if subtotal >= shipping_threshold else shipping_fee
		discount = 0.0
		coupon_code = None

		if bucket_index > 0:
			coupon = coupons[bucket_index - 1]
			if subtotal >= coupon["min"]:
				discount = coupon["off"]
				coupon_code = coupon["code"]

		total_discount += discount
		total_shipping += shipping

		order_details.append(
			{
				"indexes": orders[bucket_index][:],
				"subtotal": round(subtotal, 2),
				"coupon": coupon_code,
				"discount": round(discount, 2),
				"shipping": round(shipping, 2),
			}
		)

	net = round(total_discount - total_shipping, 2)
	return net, order_details


def find_best_split(items, coupons, shipping_threshold=100.0, shipping_fee=10.0):
	"""
	Brute force with recursion.
	For each item, choose one of 4 buckets.
	4 buckets => 4^n combinations.
	Fine for this small assignment size.
	"""
	n = len(items)
	orders = [[], [], [], []]

	best_net = -10**9
	best_details = None

	def backtrack(item_index):
		nonlocal best_net, best_details

		if item_index == n:
			net, details = evaluate_orders(
				items,
				orders,
				coupons,
				shipping_threshold,
				shipping_fee,
			)
			if net > best_net:
				best_net = net
				best_details = details
			return

		for bucket in range(4):
			orders[bucket].append(item_index)
			backtrack(item_index + 1)
			orders[bucket].pop()

	backtrack(0)
	return best_net, best_details


def print_result(items, best_net, best_details):
	print("=== Best Split ===")
	total_discount = 0.0
	total_shipping = 0.0

	for i, order in enumerate(best_details, 1):
		print("\nOrder", i)
		for idx in order["indexes"]:
			print(" -", items[idx]["name"], "$" + format(items[idx]["price"], ".2f"))

		print(" Subtotal: $" + format(order["subtotal"], ".2f"))
		if order["coupon"] is None:
			print(" Coupon: none")
		else:
			print(" Coupon:", order["coupon"], "(-$" + format(order["discount"], ".2f") + ")")
		print(" Shipping: $" + format(order["shipping"], ".2f"))

		total_discount += order["discount"]
		total_shipping += order["shipping"]

	print("\nTotal coupon discount: $" + format(total_discount, ".2f"))
	print("Total shipping: $" + format(total_shipping, ".2f"))
	print("Best net savings: $" + format(best_net, ".2f"))


if __name__ == "__main__":
	items = [
		{"name": "Hobbywing Xerun XR10 Stock Spec G2 ESC", "price": 119.99},
		{"name": "Hobbywing Xerun V10 G4R Motor (13.5T)", "price": 149.99},
		{"name": "Team Associated RC10T7 Team Kit", "price": 429.99},
		{"name": "Factory Team Silicone Shock Oil 25wt", "price": 9.99},
		{"name": "Factory Team Silicone Shock Oil 27.5wt", "price": 9.99},
		{"name": "12mm Hex 2.2 Rear Hex Wheels", "price": 8.99},
		{"name": "2.2 Front Buggy Wheels w/12mm Hex", "price": 8.99},
		{"name": "JConcepts Smoothie 2 Rear Buggy Tires", "price": 21.75},
		{"name": "JConcepts Smoothie 2 2WD Front Buggy Tires", "price": 21.75},
		{"name": "Reedy Zappers HV SG6 2S Battery", "price": 80.99},
	]

	coupons = [
		{"code": "MAR1026", "min": 100.0, "off": 10.0},
		{"code": "MAR1526", "min": 125.0, "off": 15.0},
		{"code": "MAR3026", "min": 350.0, "off": 30.0},
	]

	best_net, best_details = find_best_split(
		items,
		coupons,
		shipping_threshold=100.0,
		shipping_fee=10.0,
	)

	print_result(items, best_net, best_details)
