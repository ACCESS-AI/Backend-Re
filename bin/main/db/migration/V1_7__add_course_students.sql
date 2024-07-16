create table course_registered_students (
  course_id bigint not null,
  registered_students varchar(255));

alter table if exists course_registered_students add constraint FKzieleeKaizuY5OoS7Xuab9eame foreign key (course_id) references course;

