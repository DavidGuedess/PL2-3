import express from 'express';
import authRoutes from './routes/auth';

const app = express();
const PORT = process.env.PORT || 3001;

app.use(express.json());

app.use('/api/auth', authRoutes);

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
