-- Migration: Create GridResumes table for syncing grid-based resume designs
-- Date: 2025-12-02
-- Description: Replaces form-based Resume sync with GridResume sync using JSONB

-- Create GridResumes table
CREATE TABLE IF NOT EXISTS "GridResumes" (
    id TEXT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    name TEXT NOT NULL DEFAULT 'Untitled Resume',
    "gridResumeData" JSONB NOT NULL,
    thumbnail TEXT,
    "createdAt" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    "updatedAt" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT fk_user FOREIGN KEY ("userId") REFERENCES "Users"(id) ON DELETE CASCADE
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_grid_resumes_user_id ON "GridResumes"("userId");
CREATE INDEX IF NOT EXISTS idx_grid_resumes_updated_at ON "GridResumes"("updatedAt" DESC);

-- Comments for documentation
COMMENT ON TABLE "GridResumes" IS 'Stores grid-based resume designs with complete layout and styling information';
COMMENT ON COLUMN "GridResumes".id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN "GridResumes"."userId" IS 'Foreign key to Users table';
COMMENT ON COLUMN "GridResumes".name IS 'Resume name/title (extracted for quick queries)';
COMMENT ON COLUMN "GridResumes"."gridResumeData" IS 'Complete GridResume structure as JSONB (pages, elements, styles, etc.)';
COMMENT ON COLUMN "GridResumes".thumbnail IS 'Base64 encoded thumbnail image for list preview';
COMMENT ON COLUMN "GridResumes"."createdAt" IS 'Creation timestamp';
COMMENT ON COLUMN "GridResumes"."updatedAt" IS 'Last update timestamp (for sync tracking)';
