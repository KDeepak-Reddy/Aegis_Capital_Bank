FROM node:18-alpine
WORKDIR /app
COPY package.json ./
RUN npm install --production
COPY . .
EXPOSE 5502
CMD ["npx", "http-server", ".", "-p", "5502", "-c-1"]
