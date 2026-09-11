# Placeholder Dockerfile for frontend
FROM node:18-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install --silent
COPY frontend ./
RUN npm run build || true

FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
