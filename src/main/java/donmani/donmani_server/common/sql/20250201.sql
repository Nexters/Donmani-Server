// Use DBML to define your database structure
// Docs: https://dbml.dbdiagram.io/docs

Table Expense {
  id bigint [primary key]
  user_id bigint
  created_at timestamp
  category category_type
  flag flag_type
  memo string
}

Table User {
  id bigint [primary key]
  user_key string
  name string
  level int
}

Enum category_type {
  created
  running
  done
  failure
}

Enum flag_type {
  GOOD
  BAD
  NO
}

Ref: Expense.user_id > User.id // many-to-one
