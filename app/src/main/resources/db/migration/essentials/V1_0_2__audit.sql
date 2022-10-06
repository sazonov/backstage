create table if not exists audit(
    id varchar(40) primary key,
    type varchar(512) not null,
    object_id varchar(40) not null,
    user_id varchar(40),
    success boolean default true,
    date timestamp not null,
    properties jsonb
);

create index ix_audit_type on audit(type);
create index ix_audit_object_id on audit(object_id);
create index ix_audit_user_id on audit(user_id);
create index ix_audit_date on audit(date);