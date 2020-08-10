mysql -u root -p

create user chk386@'%' identified by 'Cdr0m38^';
GRANT ALL PRIVILEGES ON test.* TO 'chk386'@'%';

drop table commit_log
create table commit_log
(
    no int auto_increment
        primary key,
    author varchar(100) not null,
    month numeric not null,
    url varchar(500) not null,
    title varchar(3000) not null,
    modified_line_count numeric not null,
    deleted_line_count numeric not null,
    file_changed_count numeric not null,
    file_changed_name_compressed varchar(800)
);

create index commit_log_idx
    on commit_log (author, month, modified_line_count, file_changed_count, file_changed_name_compressed);

insert into commit_log(author, month, url, title, modified_line_count, deleted_line_count, file_changed_count, file_changed_name_compressed)
values ('', 1, '', '', 1, 1, 1, '');

update commit_log
   set modified_line_count = 0
     , deleted_line_count = 0
     , file_changed_count = 0
     , file_changed_name_compressed = ''
 where no = 1;

delete from commit_log where no > 0;

select * from commit_log;

commit;




