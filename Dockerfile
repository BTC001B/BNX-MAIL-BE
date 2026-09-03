FROM node:20-alpine WORKDIR /app

# Copy dependency files
COPY package*.json ./

# Install production dependencies
RUN npm ci --omit=dev

# Copy application source code
COPY . .

# Expose the application port
EXPOSE 3000

# Start command
CMD ["npm", "start"]