```mermaid
erDiagram
    USER ||--|| USER_AUTH : has
    USER ||--|| WALLET : has
    WALLET ||--o{ WALLET_TRANSACTION : has
    WALLET ||--o{ WITHDRAWAL : has
    USER ||--o{ PAYMENT : makes
    PAYMENT ||--|| CHARGE_CHECK : has
    USER ||--o{ REPORT : reports
    USER ||--o{ NOTIFICATION : receives

    CATEGORY ||--o{ CATEGORY : parent_of
    USER ||--o{ CATEGORY_SUBSCRIPTION : subscribes
    CATEGORY ||--o{ CATEGORY_SUBSCRIPTION : subscribed_by

    USER ||--o{ ITEM : sells
    CATEGORY ||--o{ ITEM : categorizes
    ITEM ||--o{ ITEM_IMAGE : has
    USER ||--o{ WISH : wishes
    ITEM ||--o{ WISH : wished_by

    ITEM ||--|| AUCTION : has
    AUCTION ||--o{ BID : has
    USER ||--o{ BID : bids
    AUCTION }o--|| USER : current_bidder

    ORDER ||--|| AUCTION : based_on
    ORDER ||--|| ITEM : includes
    ORDER }o--|| USER : buyer
    ORDER }o--|| USER : seller
    ORDER ||--|| SHIPMENT : has

    AUCTION ||--|| CHAT_ROOM : has
    USER ||--o{ CHAT_ROOM : seller_in
    USER ||--o{ CHAT_ROOM : buyer_in
    CHAT_ROOM ||--o{ CHAT_MESSAGE : has
    USER ||--o{ CHAT_MESSAGE : sends

    COUPON ||--o{ COUPON_ISSUED : issues
    USER ||--o{ COUPON_ISSUED : receives

    ORDER ||--|| SETTLEMENT : settles

    CHAT_SCENARIO ||--o{ CHAT_NODE : has
    CHAT_NODE ||--o{ CHAT_OPTION : has
    CHAT_OPTION }o--|| CHAT_NODE : next_node

    USER {
        bigint user_id PK
        string email
        string nickname
        double latitude
        double longitude
        string location
    }

    USER_AUTH {
        bigint auth_id PK
        bigint user_id FK
        string auth_type
        string identifier
    }

    WALLET {
        bigint wallet_id PK
        bigint user_id FK
        decimal balance
        decimal locked_balance
    }

    WALLET_TRANSACTION {
        bigint transaction_id PK
        bigint wallet_id FK
        decimal amount
        string type
    }

    WITHDRAWAL {
        bigint withdrawal_id PK
        bigint wallet_id FK
        decimal amount
        string status
    }

    CATEGORY {
        bigint category_id PK
        bigint parent_id FK
        string name
    }

    CATEGORY_SUBSCRIPTION {
        bigint category_subscription_id PK
        bigint user_id FK
        bigint category_id FK
    }

    ITEM {
        bigint item_id PK
        bigint seller_id FK
        bigint category_id FK
        string title
        string item_location
    }

    ITEM_IMAGE {
        bigint item_image_id PK
        bigint item_id FK
        string image_url
    }

    WISH {
        bigint wish_id PK
        bigint user_id FK
        bigint item_id FK
    }

    AUCTION {
        bigint auction_id PK
        bigint item_id FK
        bigint current_bidder_id FK
        string status
    }

    BID {
        bigint bid_id PK
        bigint auction_id FK
        bigint bidder_id FK
        decimal bid_price
    }

    ORDER {
        bigint order_id PK
        bigint auction_id FK
        bigint item_id FK
        bigint buyer_id FK
        bigint seller_id FK
        string status
    }

    SHIPMENT {
        bigint shipment_id PK
        bigint order_id FK
        string status
    }

    PAYMENT {
        bigint payment_id PK
        bigint user_id FK
        decimal amount
        string status
    }

    CHARGE_CHECK {
        bigint id PK
        bigint user_id FK
        bigint payment_id FK
        string status
    }

    REPORT {
        bigint report_id PK
        bigint reporter_id FK
        string target_type
        bigint target_id
    }

    NOTIFICATION {
        bigint notification_id PK
        bigint user_id FK
        string type
        string ref_type
        bigint ref_id
    }

    COUPON {
        bigint id PK
        string name
        decimal amount
    }

    COUPON_ISSUED {
        bigint id PK
        bigint coupon_id FK
        bigint user_id FK
        string status
    }

    CHAT_ROOM {
        bigint chat_room_id PK
        bigint auction_id FK
        bigint seller_id FK
        bigint buyer_id FK
    }

    CHAT_MESSAGE {
        bigint chat_message_id PK
        bigint room_id FK
        bigint sender_id FK
        string message
    }

    SETTLEMENT {
        bigint settlement_id PK
        bigint order_id FK
        bigint buyer_id
        bigint seller_id
        bigint platform_user_id
    }

    CHAT_SCENARIO {
        bigint id PK
        string scenario_key
        string title
    }

    CHAT_NODE {
        bigint id PK
        bigint scenario_id FK
        string node_key
    }

    CHAT_OPTION {
        bigint id PK
        bigint node_id FK
        bigint next_node_id FK
        string label
    }

    DAILY_STATISTICS {
        bigint id PK
        date stat_date
    }

    ASYNC_TASK {
        bigint id PK
        string request_id
        bigint user_id
        bigint auction_id
    }

    INQUIRY {
        bigint inquiry_id PK
        string email
        string nickname
    }
```
