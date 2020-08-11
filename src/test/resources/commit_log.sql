mysql -u root

CREATE DATABASE test;
create user chk386@'%' identified by 'Cdr0m38^';
GRANT ALL PRIVILEGES ON test.* TO 'chk386'@'%';

drop table commit_log;
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


create temporary table tmp (file_change_name_compressed varchar(600));
create index tmp_tdx on tmp (file_change_name_compressed);

create temporary table tmp2 (file_change_name_compressed varchar(600));
create index tmp_tdx2 on tmp2 (file_change_name_compressed);

create temporary table tmp3 (file_change_name_compressed varchar(600));
create index tmp_tdx3 on tmp3 (file_change_name_compressed);

insert into tmp
select file_changed_name_compressed
from commit_log
where file_changed_count > 0
group by file_changed_name_compressed
having count(file_changed_name_compressed) > 1;



# // 1474
select count(*)
  from commit_log a
 inner join tmp b on a.file_changed_name_compressed = b.file_change_name_compressed;

# 1239
delete from tmp2 where length(file_change_name_compressed) > 0;
insert into tmp2
select distinct (b.file_changed_name_compressed)
  from commit_log a
 inner join commit_log b on a.file_changed_name_compressed = b.file_changed_name_compressed
                        and a.modified_line_count = b.modified_line_count
                        and a.deleted_line_count = b.deleted_line_count
                        and a.no != b.no;


select * from tmp;
select * from tmp2;

select *from tmp a left join tmp2 b on a.file_change_name_compressed = b.file_change_name_compressed where b.file_change_name_compressed is null

select * from commit_log where file_changed_name_compressed = '78daadd2cd6ec2300c00e057d9036cf1330002699ad861178e281853b9e44f715ac4dbe3b0562a9b905669b7248e3fd971307a1f034846f09603b4b6b780d143c0a427cee9269cb8e9b22dac1729745e60ab81d5f4fc83aea6a6bebee04cd0c72339582456afe438b00376a183b147cfc108e59e919eb86374f42be4288f750edb7f44bf48ca1c58433625d8ad976fef9f1be8992edfaeb6cf27c6fb5b9856d25f14297a1def35b65285ced144d349c8c0247ed2dbc30ca4c2b07411cf1c9a0dbbf2d8d21c6547876d8fbf86f8f3576492d865242d9f446ca38bfa2626e5982817269995b76fed24f506067d0f9d';

select row_number() over (partition by file_changed_name_compressed order by a.title desc) as ranking, a.no, a.author, a.month, a.url, a.title, a.modified_line_count, a.deleted_line_count, a.file_changed_count,
     file_changed_name_compressed,
       row_number() over (partition by file_changed_name_compressed order by a.title desc) as ranking
from commit_log a
where modified_line_count != 0 and deleted_line_count != 0 and file_changed_count != 0 and modified_line_count <= 10000

