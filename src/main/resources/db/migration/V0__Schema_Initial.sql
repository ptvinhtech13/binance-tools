create sequence candlestick_data_seq MINVALUE 1 START WITH 1 INCREMENT BY 1000 NO CYCLE;

create table candlestick_data
(
    id                 bigint not null
        constraint candlestick_data_pk
            primary key,
    open_time          timestamp,
    symbol          varchar(100),
    open_price         numeric,
    high_price         numeric,
    low_price          numeric,
    close_price        numeric,
    volume             numeric,
    close_time         timestamp,
    base_asset_volume  numeric,
    quote_asset_volume numeric
);

create unique index if not exists candlestick_data_id_uindex
    on candlestick_data (id);

create index if not exists candlestick_data_close_price_index
    on candlestick_data (open_time);

create index if not exists candlestick_data_close_price_index
    on candlestick_data (close_time);

create index if not exists candlestick_data_close_price_index
    on candlestick_data (close_price);

create index if not exists candlestick_data_close_time_index
    on candlestick_data (close_time);

create index if not exists candlestick_data_high_price_index
    on candlestick_data (high_price);

create index if not exists candlestick_data_low_price_index
    on candlestick_data (low_price);

create index if not exists candlestick_data_open_price_index
    on candlestick_data (open_price);

create sequence if not exists candlestick_data_seq MINVALUE 1 START WITH 1 INCREMENT BY 1000 NO CYCLE;