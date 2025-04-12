create table if not exists test
(
	id                      String,
	date                    DateTime,
	updated                 DateTime,
	completed               Nullable(DateTime),
	testInt                 UInt32,
	testInt8                UInt8,
	nullableString          Nullable(String),
	arrayIds                Array(String),
	string                  String,
	arrayOfString           Array(String)
)
engine = ReplacingMergeTree(updated)
primary key (id)
partition by toYYYYMMDD(date)
order by (id)