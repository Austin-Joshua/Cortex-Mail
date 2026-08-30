import React from 'react';
import { Navigate } from 'react-router-dom';

/** Shared/team threads are not shipped — keep the route but send people home. */
export const SharedPage: React.FC = () => <Navigate to="/dashboard" replace />;
