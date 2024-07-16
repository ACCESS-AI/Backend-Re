create table global_file (
  id bigint not null,
  is_binary boolean not null,
  editable boolean not null,
  enabled boolean not null,
  grading boolean not null,
  instruction boolean not null,
  language varchar(255) not null,
  name varchar(255) not null,
  path varchar(255) not null,
  solution boolean not null,
  template text not null,
  visible boolean not null,
  course_id bigint not null,
  primary key (id));

create sequence global_file_seq start with 1 increment by 50;

alter table if exists global_file add constraint FKAoGai9iXuoch6shinegh foreign key (course_id) references course;
