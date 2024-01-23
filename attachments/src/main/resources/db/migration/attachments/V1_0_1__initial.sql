create table if not exists attachment
(
	id        		varchar(40) 					primary key,
	created   		timestamp    		not null,
	file_name 		varchar(255),
	mime_type 		varchar(255) 		not null,
	size      		integer      		not null,
	user_id   		varchar(255),
	updated   		timestamp,
	checksum  		varchar(255)
);

create index if not exists ix_attachment_user_id on attachment (user_id);

create table if not exists attachment_binding
(
	id            	varchar(40) 						primary key,
	created       	timestamp 			not null,
	object_id     	varchar(255),
	type          	varchar(255),
	user_id       	varchar(255),
	fk_attachment 	varchar(40) 						references attachment (id)
);

create index if not exists ix_attachment_binding_fk_attachment_binding_fk_attachment on attachment_binding (fk_attachment);
create index if not exists ix_attachment_binding_type on attachment_binding (type);
create index if not exists ix_attachment_binding_user_id on attachment_binding (user_id);
create index if not exists ix_attachment_binding_object_id on attachment_binding (object_id);
