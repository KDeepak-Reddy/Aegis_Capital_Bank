FROM node:18-alpine
WORKDIR /app
COPY package.json ./
RUN npm install --production
COPY . .
EXPOSE 5173
CMD ["npx", "serve", ".", "-l", "5173"]
