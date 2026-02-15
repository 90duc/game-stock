# AGENTS.md - Coding Guidelines for game-stock

## Build & Run Commands

### Maven Commands
```bash
# Compile the project
mvn compile

# Package into JAR (copies dependencies to target/lib)
mvn package

# Run the application
mvn exec:java

# Clean build artifacts
mvn clean

# Full clean build
mvn clean package
```

### Manual Compilation (Alternative)
```bash
# Compile all Java files
javac -cp "lib/*" -d out src/main/java/com/stockgame/**/*.java

# Run the application
java -cp "out:lib/*" com.stockgame.ui.StockGameApp
```

### Scripts
- `./build.sh` or `build.bat` - Build project (compiles Java sources)
- `./run.sh` or `run.bat` - Run application

## Testing

**No test framework currently configured.** To add tests:
1. Add JUnit 5 to pom.xml dependencies
2. Create `src/test/java/com/stockgame/` directory
3. Run single test: `mvn test -Dtest=ClassName#methodName`
4. Run all tests: `mvn test`

## Code Style Guidelines

### Project Structure
```
src/main/java/com/stockgame/
├── model/          # Entity classes (POJOs) - User, Stock, Order, Position, etc.
├── dao/            # Data Access Objects - database operations
├── service/        # Business logic layer - StockTradingService
├── ui/             # JavaFX UI components - views and main app
├── util/           # Utility classes - DatabaseUtil
└── init/           # Data initialization - DataInitializer
```

### Naming Conventions
- **Classes**: PascalCase (e.g., `StockTradingService`, `OrderDao`, `DayKLine`)
- **Methods**: camelCase (e.g., `getById()`, `startGame()`, `getPendingOrdersByStock()`)
- **Variables**: camelCase (e.g., `stockId`, `currentPrice`, `gameSessionId`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `GAME_DURATION_MINUTES`, `PRICE_LIMIT`)
- **Enums**: PascalCase with UPPER_SNAKE_CASE values (e.g., `OrderType.BUY`, `OrderStatus.PENDING`)
- **Packages**: lowercase, single word if possible

### Import Organization
Order: Java stdlib → third-party → JavaFX → project imports

```java
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import com.stockgame.model.Order;
import com.stockgame.dao.OrderDao;

import javafx.application.Application;
import javafx.scene.control.*;
```

### Formatting
- Line length: 120 chars max
- Braces: K&R style (opening on same line)
- Indentation: 4 spaces (no tabs)
- Blank lines: between logical sections and methods

### Types & Money
- **Always use `BigDecimal` for money** - never `double` or `float`
- Set scale: `price.setScale(2, RoundingMode.HALF_UP)`
- Use wrapper types (`Long`, `Integer`) for nullable DB fields
- Use `LocalDateTime` for timestamps

### Error Handling
- **DAO**: throw `SQLException`
- **Service**: wrap in `RuntimeException` with Chinese messages
- **UI**: try-catch, print to `System.err`
- **Connections**: always use try-with-resources

### Code Patterns

#### Model Classes (POJOs)
- Private fields with getters/setters
- Default constructor for deserialization
- Business logic methods for calculations
```java
public class Order {
    public enum OrderType { BUY, SELL }
    public enum OrderStatus { PENDING, FILLED, CANCELLED, EXPIRED }
    
    private Long id;
    private BigDecimal price;
    
    public Integer getRemainingQuantity() {
        return quantity - filledQuantity;
    }
}
```

#### DAO Classes
- Instance methods (not singleton, create new instance)
- Use `DatabaseUtil.getConnection()` for connections
- Always use `PreparedStatement` with parameters (no string concatenation)
- Map `ResultSet` to model objects in private methods
```java
public class OrderDao {
    public List<Order> getPendingOrdersByStock(Long stockId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE stock_id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stockId);
            ResultSet rs = stmt.executeQuery();
            // map results
        }
    }
}
```

#### Service Layer
- Hold instances of DAO classes
- Wrap checked exceptions in runtime exceptions
- Use `synchronized` for concurrent methods
- Business logic and transaction management

#### UI Layer
- JavaFX components (`Tab`, `TableView`, `Label`, `Button`, etc.)
- Refresh data via `Timeline` (1-second interval)
- Use `Platform.runLater()` for UI updates from background threads

### Database
- SQLite database file: `stock_game.db`
- Use `DatabaseUtil.getConnection()` for all DB access
- Use try-with-resources for auto-cleanup
- SQL table names: snake_case (e.g., `game_sessions`, `trade_records`)

### Comments
- Use Chinese comments for business logic and explanations
- Javadoc not required but helpful for public APIs
- Inline comments for complex algorithms or non-obvious logic
- Comment database queries briefly

### Git
- No pre-commit hooks configured
- Database file (`stock_game.db`) should not be committed
- IDE files already in repo: `.idea/`, `.project`, `.classpath`, `*.iml`

## Dependencies
- **Java**: 8
- **JavaFX**: 11 (controls, fxml)
- **SQLite JDBC**: 3.36.0.3
- **Jackson**: 2.13.0 (databind)
- **Hutool**: 5.7.1 (all)

## IDE Setup
- Eclipse project files: `.project`, `.classpath`
- IntelliJ IDEA files: `.idea/`, `*.iml`
- Source encoding: UTF-8
- Compiler source/target: 1.8
