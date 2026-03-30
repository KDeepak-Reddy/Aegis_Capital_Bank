FROM node:18-alpine
WORKDIR /app
COPY package.json ./
RUN npm install --production
COPY . .
EXPOSE 5501
CMD ["npx", "http-server", ".", "-p", "5501", "-c-1"]
