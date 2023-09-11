alter table audit
    alter column object_id drop not null;

update audit set object_id = null where object_id = '';