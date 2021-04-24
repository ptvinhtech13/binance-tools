create sequence if not exists coin_report_seq MINVALUE 1 START WITH 1 INCREMENT BY 1000 NO CYCLE;

create table coin_report
(
    id                 bigint not null
        constraint coin_report_pk
            primary key,
    symbol          varchar(100),
    lowest_price         numeric,
    highest_price         numeric,
    lowest_price_in_1h     numeric,
    highest_price_in_1h    numeric,
    current_price          numeric,
    percent_down        numeric,
    percent_delta             numeric,
    created_at         timestamp default now(),
    updated_at         timestamp default now()
);

create index if not exists coin_report_lowest_price_index
    on coin_report (lowest_price);

create index if not exists coin_report_created_at_index
    on coin_report (created_at);

create index if not exists coin_report_symbol_index
    on coin_report (symbol);

create index if not exists coin_report_lowest_price_in_1h_index
    on coin_report (lowest_price_in_1h);

create index if not exists coin_report_highest_price_in_1h_index
    on coin_report (highest_price_in_1h);