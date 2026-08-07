FROM nginx:1.24-alpine

MAINTAINER xuzhihao074

ENV TZ=Asia/Shanghai

RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo "${TZ}" > /etc/timezone

COPY ./dist /usr/share/nginx/html

# context 是 ruoyi-wms-frontend，所以 nginx.conf 路径是 docker/nginx.conf
COPY ./docker/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
