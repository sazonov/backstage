/*
 *    Copyright 2019-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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