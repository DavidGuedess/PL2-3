import swaggerJsdoc from 'swagger-jsdoc'

const options: swaggerJsdoc.Options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Miaugenda API',
      version: '1.0.0',
      description: 'API de gestão de clínica veterinária',
    },
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
        },
      },
    },
  },
  apis: ['./src/index.ts', './src/routes/*.ts'],
}

export const swaggerSpec = swaggerJsdoc(options)
