import { Router, Request, Response } from "express";
import { v4 as uuidv4 } from "uuid";
import { users } from "../data/users";
import { User, UserRole, ProfessionalCategory } from "../types/user";

const router = Router();

function requireAdmin(req: Request, res: Response, next: Function) {
  const role = req.headers["x-user-role"];

  if (role !== "ADMIN") {
    return res.status(403).json({
      message: "Forbidden: Admins only",
    });
  }

  next();
}

function getAuthenticatedUser(req: Request) {
  const userId = req.headers["x-user-id"];

  if (typeof userId !== "string" || userId.trim() === "") {
    return null;
  }

  const authenticatedUser = users.find(function (user) {
    return user.id === userId;
  });

  return authenticatedUser || null;
}

function sanitizeUser(user: User) {
  const { password, ...safeUser } = user;
  return safeUser;
}

router.get("/", (_req: Request, res: Response) => {
  const safeUsers = users.map(function (user) {
    return sanitizeUser(user);
  });

  return res.status(200).json(safeUsers);
});

router.get("/me", (req: Request, res: Response) => {
  const authenticatedUser = getAuthenticatedUser(req);

  if (!authenticatedUser) {
    return res.status(401).json({
      message: "Unauthorized: Authenticated user not found",
    });
  }

  return res.status(200).json(sanitizeUser(authenticatedUser));
});

router.get("/:id", requireAdmin, (req: Request, res: Response) => {
  const { id } = req.params;
  const user = users.find(function (user) {
    return user.id === id;
  });

  if (!user) {
    return res.status(404).json({
      message: "User not found",
    });
  }

  return res.status(200).json(sanitizeUser(user));
});

router.post("/", requireAdmin, (req: Request, res: Response) => {
  const {
    fullName,
    email,
    employeeNumber,
    role,
    professionalCategory,
    contact,
    password,
  } = req.body;

  const validRoles: UserRole[] = ["ADMIN", "MANAGER", "EMPLOYEE"];

  const validProfessionalCategories: ProfessionalCategory[] = [
    "FILLER1",
    "FILLER2",
    "FILLER3",
    "FILLER4",
  ];

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (typeof fullName !== "string" || fullName.trim() === "") {
    return res.status(400).json({
      message: "fullName is required and must be a non-empty string",
    });
  }

  if (
    typeof email !== "string" ||
    !emailRegex.test(email.trim().toLowerCase())
  ) {
    return res.status(400).json({
      message: "Invalid email format",
    });
  }

  if (typeof employeeNumber !== "string" || employeeNumber.trim() === "") {
    return res.status(400).json({
      message: "employeeNumber is required and must be a non-empty string",
    });
  }

  if (typeof role !== "string" || !validRoles.includes(role as UserRole)) {
    return res.status(400).json({
      message: "Invalid role",
    });
  }

  if (
    typeof professionalCategory !== "string" ||
    !validProfessionalCategories.includes(
      professionalCategory as ProfessionalCategory,
    )
  ) {
    return res.status(400).json({
      message: "Invalid professional category",
    });
  }

  if (typeof contact !== "string" || contact.trim() === "") {
    return res.status(400).json({
      message: "contact is required and must be a non-empty string",
    });
  }

  if (typeof password !== "string" || password.trim() === "") {
    return res.status(400).json({
      message: "password is required and must be a non-empty string",
    });
  }

  const normalizedFullName = fullName.trim();
  const normalizedEmail = email.trim().toLowerCase();
  const normalizedEmployeeNumber = employeeNumber.trim();
  const normalizedContact = contact.trim();
  const normalizedPassword = password.trim();
  const emailAlreadyExists = users.find(function (user) {
    return user.email.toLowerCase() === normalizedEmail;
  });

  if (emailAlreadyExists) {
    return res.status(409).json({
      message: "Email already exists",
    });
  }

  const employeeNumberAlreadyExists = users.find(function (user) {
    return user.employeeNumber === normalizedEmployeeNumber;
  });

  if (employeeNumberAlreadyExists) {
    return res.status(409).json({
      message: "Employee number already exists",
    });
  }

  if (typeof contact !== "string" || contact.trim() === "") {
    return res.status(400).json({
      message: "contact is required and must be a non-empty string",
    });
  }

  if (typeof password !== "string" || password.trim() === "") {
    return res.status(400).json({
      message: "password is required and must be a non-empty string",
    });
  }

  const newUser: User = {
    id: uuidv4(),
    fullName: normalizedFullName,
    email: normalizedEmail,
    employeeNumber: normalizedEmployeeNumber,
    role: role as UserRole,
    professionalCategory: professionalCategory as ProfessionalCategory,
    contact: normalizedContact,
    password: normalizedPassword,
    isActive: true,
    createdAt: new Date().toISOString(),
  };

  users.push(newUser);

  return res.status(201).json(sanitizeUser(newUser))
});

router.patch("/:id/deactivate", requireAdmin, (req: Request, res: Response) => {
  const { id } = req.params;

  const user = users.find(function (user) {
    return user.id === id;
  });

  if (!user) {
    return res.status(404).json({
      message: "User not found",
    });
  }

  if (!user.isActive) {
    return res.status(409).json({
      message: "User already deactivated",
    });
  }

  user.isActive = false;

  return res.status(200).json(sanitizeUser(user))
});

router.patch("/me", (req: Request, res: Response) => {
  const authenticatedUser = getAuthenticatedUser(req);

  if (!authenticatedUser) {
    return res.status(401).json({
      message: "Unauthorized: Authenticated user not found",
    });
  }

  const { fullName, contact, password } = req.body;

  if (
    fullName === undefined &&
    contact === undefined &&
    password === undefined
  ) {
    return res.status(400).json({
      message:
        "At least one field must be provided: fullName, contact or password",
    });
  }

  if (fullName !== undefined) {
    if (typeof fullName !== "string" || fullName.trim() === "") {
      return res.status(400).json({
        message: "fullName must be a non-empty string",
      });
    }

    authenticatedUser.fullName = fullName.trim();
  }

  if (contact !== undefined) {
    if (typeof contact !== "string" || contact.trim() === "") {
      return res.status(400).json({
        message: "contact must be a non-empty string",
      });
    }

    authenticatedUser.contact = contact.trim();
  }

  if (password !== undefined) {
    if (typeof password !== "string" || password.trim() === "") {
      return res.status(400).json({
        message: "password must be a non-empty string",
      });
    }

    authenticatedUser.password = password.trim();
  }

  return res.status(200).json(sanitizeUser(authenticatedUser));
});

export default router;
