create table if not exists document_template
(
	id                  varchar(40)         primary key,
    name                varchar(500)        not null,
    attachment_id		varchar(40)         not null,
    created             timestamp           not null,
    updated             timestamp           not null,
    deleted             timestamp
);

create extension if not exists pg_trgm;

create index if not exists ix_fk_document_template_attachment_id
    on document_template (attachment_id);

create index if not exists ix_document_template_name_lower_trigram
    on document_template using gin (lower(name) gin_trgm_ops);