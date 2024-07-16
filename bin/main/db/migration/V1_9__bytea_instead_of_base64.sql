-- rename language to mime_type for files
alter table task_file
  rename language to mime_type;

alter table global_file
  rename language to mime_type;

-- move base64-encoded task_file and global_file data to dedicated BYTEA column
alter table task_file
  add column template_binary bytea;

alter table task_file
  alter column template drop not null;

do $$
declare
    rec record;
    base64_data text;
begin
    for rec in select id, template, is_binary from task_file loop
        if rec.is_binary then
            base64_data := substring(rec.template from 'base64,(.*)$');
            update task_file
            set template_binary = decode(base64_data, 'base64'),
                template = null
            where id = rec.id;
        else
            update task_file
            set template_binary = null
            where id = rec.id;
        end if;
    end loop;
end $$;

alter table task_file
  drop column is_binary;

alter table task_file
  add constraint either_binary_or_not check (
    (template is not null and template_binary is null) or
    (template is null and template_binary is not null)
);

alter table global_file
  add column template_binary bytea;

alter table global_file
  alter column template drop not null;

do $$
declare
    rec record;
begin
    for rec in select id, template, is_binary from global_file loop
        if rec.is_binary then
            update global_file
            set template_binary = decode(rec.template, 'base64'),
                template = null
            where id = rec.id;
        else
            update global_file
            set template_binary = null
            where id = rec.id;
        end if;
    end loop;
end $$;

alter table global_file
  drop column is_binary;

alter table global_file
  add constraint either_binary_or_not check (
    (template is not null and template_binary is null) or
    (template is null and template_binary is not null)
);
