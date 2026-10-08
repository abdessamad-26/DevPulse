FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
# `npm ci` installs exactly what package-lock.json pins (reproducible, same as CI).
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist/devpulse/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
