create table attachment (id varchar(40) not null, created timestamp not null, file_name varchar(255), mime_type varchar(255) not null, size integer not null, user_id varchar(255), primary key (id));
create index ix_attachment_user_id on attachment (user_id);
create table attachment_binding (id varchar(40) not null, created timestamp not null, object_id varchar(255), type varchar(255), user_id varchar(255), fk_attachment varchar(40), primary key (id));
create index ix_attachment_binding_fk_attachment_binding_fk_attachment on attachment_binding (fk_attachment);
create index ix_attachment_binding_type on attachment_binding (type);
create index ix_attachment_binding_user_id on attachment_binding (user_id);
create index ix_attachment_binding_object_id on attachment_binding (object_id);
alter table attachment_binding add constraint fk_attachment_binding_fk_attachment foreign key (fk_attachment) references attachment (id);