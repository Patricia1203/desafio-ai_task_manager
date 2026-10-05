# syntax=docker/dockerfile:1

# ---- Build stage -------------------------------------------------------------
FROM node:24-alpine AS build

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime stage -----------------------------------------------------------
FROM nginx:1.29-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80