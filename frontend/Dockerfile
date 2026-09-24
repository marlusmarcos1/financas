# syntax=docker/dockerfile:1
FROM node:20-alpine AS build
WORKDIR /build
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=5s --start-period=15s --retries=5 \
    CMD wget -qO- http://localhost:80/ || exit 1
