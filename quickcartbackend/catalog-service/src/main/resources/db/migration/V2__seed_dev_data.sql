INSERT INTO categories (name, description) VALUES
    ('Fruits & Vegetables', 'Fresh fruits and vegetables'),
    ('Dairy & Eggs',        'Milk, curd, paneer and eggs'),
    ('Bakery',              'Bread, buns and baked goods'),
    ('Snacks',              'Chips, biscuits and namkeen'),
    ('Beverages',           'Juices, soft drinks, tea and water');

INSERT INTO products (category_id, name, description, price, unit)
SELECT c.id, p.name, p.description, p.price, p.unit
FROM (VALUES
    ('Fruits & Vegetables', 'Banana',                 'Fresh ripe bananas',          49.00, '1 dozen'),
    ('Fruits & Vegetables', 'Tomato',                 'Farm fresh tomatoes',         30.00, '500 g'),
    ('Fruits & Vegetables', 'Onion',                  'Fresh red onions',            40.00, '1 kg'),
    ('Fruits & Vegetables', 'Apple',                  'Crisp red apples',           180.00, '1 kg'),
    ('Dairy & Eggs',        'Toned Milk',             'Fresh toned milk',            28.00, '500 ml'),
    ('Dairy & Eggs',        'Curd',                   'Thick set curd',              40.00, '400 g'),
    ('Dairy & Eggs',        'Eggs',                   'Farm eggs',                   54.00, '6 pcs'),
    ('Dairy & Eggs',        'Paneer',                 'Soft fresh paneer',           90.00, '200 g'),
    ('Bakery',              'Brown Bread',            'Whole wheat sliced bread',    45.00, '400 g'),
    ('Bakery',              'Milk Bread',             'Soft sliced milk bread',      40.00, '400 g'),
    ('Bakery',              'Butter Croissant',       'Flaky butter croissants',     60.00, '2 pcs'),
    ('Bakery',              'Burger Buns',            'Soft burger buns',            35.00, '4 pcs'),
    ('Snacks',              'Salted Potato Chips',    'Crispy salted chips',         20.00, '52 g'),
    ('Snacks',              'Roasted Peanuts',        'Salted roasted peanuts',      45.00, '200 g'),
    ('Snacks',              'Digestive Biscuits',     'Whole wheat biscuits',        55.00, '250 g'),
    ('Snacks',              'Namkeen Mix',            'Spicy savoury mix',           60.00, '200 g'),
    ('Beverages',           'Orange Juice',           'Fruit juice, no added sugar', 110.00, '1 l'),
    ('Beverages',           'Cola',                   'Carbonated soft drink',       40.00, '750 ml'),
    ('Beverages',           'Green Tea',              'Green tea bags',             140.00, '25 bags'),
    ('Beverages',           'Packaged Drinking Water','Purified drinking water',     20.00, '1 l')
) AS p (category_name, name, description, price, unit)
JOIN categories c ON c.name = p.category_name;

INSERT INTO inventory (product_id, quantity)
SELECT id, 50 FROM products;