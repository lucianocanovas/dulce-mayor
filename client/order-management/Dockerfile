# ==============================================================================
# Stage 1: Build React + TypeScript + Vite SPA (order-management)
# ==============================================================================
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_URL=/api
ARG VITE_AUTH0_DOMAIN=dev-is022ursig5yhq3k.us.auth0.com
ARG VITE_AUTH0_CLIENT_ID=CfWfvGPNV2dHB72zT1yU6dFI0eMpaQZF

ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_AUTH0_DOMAIN=${VITE_AUTH0_DOMAIN}
ENV VITE_AUTH0_CLIENT_ID=${VITE_AUTH0_CLIENT_ID}

RUN npm run build

# ==============================================================================
# Stage 2: Nginx Static Server + /api Reverse Proxy
# ==============================================================================
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

