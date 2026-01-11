#!/bin/bash

# GitHub Setup Script for Authentication Module v1.0.0
# This script helps you push your code to GitHub

echo "========================================="
echo "GitHub Repository Setup"
echo "========================================="
echo ""

# Check if gh CLI is installed
if command -v gh &> /dev/null; then
    echo "✅ GitHub CLI detected"
    echo ""
    echo "Would you like to create a new GitHub repository? (y/n)"
    read -r CREATE_REPO

    if [ "$CREATE_REPO" = "y" ] || [ "$CREATE_REPO" = "Y" ]; then
        echo ""
        echo "Creating repository..."
        gh repo create authentication-module \
            --public \
            --description "Production-ready authentication module with email/password and OAuth2 social login support" \
            --source=. \
            --remote=origin \
            --push

        echo ""
        echo "✅ Repository created and code pushed!"
        echo "🔗 Repository URL: https://github.com/$(gh api user --jq .login)/authentication-module"
    fi
else
    echo "GitHub CLI not found. Please follow manual setup:"
    echo ""
    echo "1. Go to https://github.com/new"
    echo "2. Create a new repository named 'authentication-module'"
    echo "3. Choose visibility (public/private)"
    echo "4. Do NOT initialize with README, .gitignore, or license"
    echo "5. Click 'Create repository'"
    echo ""
    echo "Then run these commands:"
    echo ""
    echo "  git remote add origin https://github.com/YOUR_USERNAME/authentication-module.git"
    echo "  git branch -M main"
    echo "  git push -u origin main"
    echo "  git push origin v1.0.0"
    echo ""
fi

echo ""
echo "========================================="
echo "Next Steps"
echo "========================================="
echo ""
echo "1. Configure GitHub Secrets for CI/CD:"
echo "   - Go to Settings > Secrets and variables > Actions"
echo "   - Add secrets:"
echo "     • DOCKER_USERNAME: Your Docker Hub username"
echo "     • DOCKER_PASSWORD: Your Docker Hub password/token"
echo ""
echo "2. Set up GitHub Environments:"
echo "   - Go to Settings > Environments"
echo "   - Create environments: development, test, production"
echo "   - Add environment-specific secrets"
echo "   - Configure protection rules for production"
echo ""
echo "3. Review and customize:"
echo "   - .github/workflows/ci-cd.yml"
echo "   - README.md (update GitHub URLs)"
echo "   - DEPLOYMENT.md"
echo ""
echo "4. Test CI/CD pipeline:"
echo "   - Make a small change and push"
echo "   - Check Actions tab in GitHub"
echo ""
echo "========================================="
echo "Documentation Links"
echo "========================================="
echo ""
echo "📖 README: README.md"
echo "🚀 Deployment: DEPLOYMENT.md"
echo "📋 Changelog: CHANGELOG.md"
echo "🏗️ Architecture: ARCHITECTURE.md"
echo ""
echo "========================================="
