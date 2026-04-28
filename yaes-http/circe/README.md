![Made for Scala 3](https://img.shields.io/badge/Scala%203-%23de3423.svg?logo=scala&logoColor=white)
![GitHub Workflow Status (with branch)](https://img.shields.io/github/actions/workflow/status/rcardin/yaes/scala.yml?branch=main)
![Maven Central](https://img.shields.io/maven-central/v/in.rcard.yaes/yaes-http-circe_3)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/rcardin/yaes)
[![javadoc](https://javadoc.io/badge2/in.rcard.yaes/yaes-http-circe_3/javadoc.svg)](https://javadoc.io/doc/in.rcard.yaes/yaes-http-circe_3)
<br/>

# λÆS HTTP Circe

JSON body encoder/decoder integration for the λÆS HTTP server using [Circe](https://circe.github.io/circe/). This module provides automatic `BodyEncoder[A]` and `BodyDecoder[A]` instances for any type that has the corresponding Circe `Encoder` or `Decoder` in scope, enabling seamless JSON request/response handling.

## Installation

Add the dependency to your `build.sbt`:

```scala
libraryDependencies += "in.rcard.yaes" %% "yaes-http-circe" % "0.17.0"
```

This module depends on `yaes-http-server` and `circe-core`/`circe-parser` (included transitively). If you want to use Circe's automatic derivation features, also include `circe-generic`:

```scala
libraryDependencies += "io.circe" %% "circe-generic" % "0.14.15"
```

## Quick Start

Import the circe encoder/decoder instances and use typed request/response bodies in your routes:

```scala
import in.rcard.yaes.*
import in.rcard.yaes.Log.given
import in.rcard.yaes.http.server.*
import in.rcard.yaes.http.circe.given
import io.circe.{Encoder, Decoder}

case class User(name: String, age: Int) derives Encoder.AsObject, Decoder

Shutdown.run {
  Log.run() {
    val server = YaesServer.route(
      // Response body automatically encoded to JSON
      GET(p"/users" / param[Int]("id")) { (req, id: Int) =>
        Response.ok(User("Alice", 30))
      },

      // Request body automatically decoded from JSON
      POST(p"/users") { req =>
        Raise.fold {
          val user = req.as[User]
          Response.created(user)
        } { case errors: List[DecodingError] =>
          Response.badRequest(errors.map(_.message).mkString(", "))
        }
      }
    )

    server.run(port = 8080)
  }
}
```

## How It Works

The module provides two separate `given` instances:

```scala
given circeBodyEncoder[A](using Encoder[A]): BodyEncoder[A]
given circeBodyDecoder[A](using Decoder[A]): BodyDecoder[A]
```

Each instance is gated on a single Circe constraint, so you can bring only what you need into scope. Importing `in.rcard.yaes.http.circe.given` brings both into scope at once.

- **`circeBodyEncoder`**: encodes values as compact JSON using `asJson.noSpaces` and sets the `Content-Type` header to `application/json`
- **`circeBodyDecoder`**: decodes JSON bodies using Circe's parser, mapping parse failures (`ParsingFailure`) to `DecodingError.ParseError` and decoding/schema failures (`DecodingFailure`) to `DecodingError.ValidationError`; all failures are raised as a non-empty `List[DecodingError]`

## Derivation Strategies

### Automatic Derivation

Use Scala 3 `derives` clauses for the simplest approach:

```scala
case class User(name: String, age: Int) derives Encoder.AsObject, Decoder
```

### Semi-Automatic Derivation

For more control, derive instances explicitly:

```scala
case class Product(id: Long, label: String)

given Encoder[Product] = Encoder.AsObject.derived
given Decoder[Product] = Decoder.derived
```

### Nested Case Classes

Both strategies work with nested structures:

```scala
case class Address(street: String, city: String) derives Encoder.AsObject, Decoder
case class Person(name: String, address: Address) derives Encoder.AsObject, Decoder

val encoder = summon[BodyEncoder[Person]]
encoder.encode(Person("Alice", Address("123 Main St", "Springfield")))
// {"name":"Alice","address":{"street":"123 Main St","city":"Springfield"}}

val decoder = summon[BodyDecoder[Person]]
// decoder.decode(body) raises List[DecodingError] on failure
```

## Error Handling

When JSON decoding fails, the codec raises a non-empty `List[DecodingError]` accumulating all errors found in the body. Use `Raise.fold` to handle decoding errors in routes:

```scala
POST(p"/users") { req =>
  Raise.fold {
    val user = req.as[User]
    Response.created(user)
  } { case errors: List[DecodingError] =>
    Response.badRequest(errors.map(_.message).mkString(", "))
  }
}
```

Common failure scenarios:
- **Malformed JSON** (e.g., `"not json at all"`) — returns a parse error
- **Missing required fields** (e.g., `{"name":"Alice"}` for a type requiring `name` and `age`) — returns a decoding error

## Requirements

- **Java 25+**: Required by λÆS for virtual threads and structured concurrency
- **Scala 3.8.1+**: Uses Scala 3 features (context functions, derives clauses, etc.)
- **yaes-http-server**: Depends on the HTTP server module for `BodyEncoder`, `BodyDecoder`, and `DecodingError`

## Contributing

Contributions to the λÆS project are welcome! Please feel free to submit pull requests or open issues if you find bugs or have feature requests.
